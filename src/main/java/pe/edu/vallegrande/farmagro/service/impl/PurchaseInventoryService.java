package pe.edu.vallegrande.farmagro.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.vallegrande.farmagro.model.Product;
import pe.edu.vallegrande.farmagro.model.sql.Purchase;
import pe.edu.vallegrande.farmagro.model.sql.PurchaseDetail;
import pe.edu.vallegrande.farmagro.dto.PurchaseDetailDTO;
import pe.edu.vallegrande.farmagro.repository.ProductRepository;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
class PurchaseInventoryService {

    private static final int MIN_STOCK_VALUE = 0;

    private final ProductRepository productRepository;

    // Stock enters only when the goods are received (PAID); a PENDING purchase is just an order.
    // No shared Mongo/SQL transaction: stock is reverted if SQL fails.
    Mono<Void> adjustForCreatedPurchase(Purchase purchase, List<PurchaseDetailDTO> details) {
        if (!AppConstants.STATUS_PAID.equals(purchase.getStatus())) return Mono.empty();
        List<PurchaseDetailDTO> adjustedDetails = new ArrayList<>();
        return Flux.fromIterable(details)
                .concatMap(detail -> adjustProductStock(detail.getMongoIdProduct(),
                        detail.getMongoIdPresentation(), detail.getMongoIdBatch(), detail.getQuantity())
                        .thenReturn(detail)
                        .doOnNext(adjustedDetails::add))
                .then()
                .onErrorResume(error -> rollbackCreatedPurchase(adjustedDetails, error)
                        .then(Mono.error(error)));
    }

    Mono<Void> adjustPurchaseDetailsWithRollback(List<PurchaseDetail> details, int direction) {
        List<PurchaseDetail> adjustedDetails = new ArrayList<>();
        return Flux.fromIterable(details)
                .concatMap(detail -> adjustProductStock(detail.getMongoIdProduct(),
                        detail.getMongoIdPresentation(), detail.getMongoIdBatch(), direction * detail.getQuantity())
                        .thenReturn(detail)
                        .doOnNext(adjustedDetails::add))
                .then()
                .onErrorResume(error -> rollbackPurchaseDetails(adjustedDetails, -direction, error)
                        .then(Mono.error(error)));
    }

    private Mono<Void> rollbackCreatedPurchase(List<PurchaseDetailDTO> details, Throwable originalError) {
        List<PurchaseDetailDTO> reverseOrder = new ArrayList<>(details);
        Collections.reverse(reverseOrder);
        return Flux.fromIterable(reverseOrder)
                .concatMap(detail -> adjustProductStock(detail.getMongoIdProduct(),
                        detail.getMongoIdPresentation(), detail.getMongoIdBatch(), -detail.getQuantity()))
                .onErrorContinue((rollbackError, detail) -> log.error(
                        "Failed to compensate inventory after purchase stock failure; original error: {}",
                        originalError.getMessage(), rollbackError))
                .then();
    }

    private Mono<Void> rollbackPurchaseDetails(List<PurchaseDetail> details, int direction,
                                               Throwable originalError) {
        List<PurchaseDetail> reverseOrder = new ArrayList<>(details);
        Collections.reverse(reverseOrder);
        return Flux.fromIterable(reverseOrder)
                .concatMap(detail -> adjustProductStock(detail.getMongoIdProduct(),
                        detail.getMongoIdPresentation(), detail.getMongoIdBatch(), direction * detail.getQuantity()))
                .onErrorContinue((rollbackError, detail) -> log.error(
                        "Failed to compensate purchase inventory after error: {}",
                        originalError.getMessage(), rollbackError))
                .then();
    }

    Mono<Void> adjustProductStock(String productId, String presentationId, String batchId, int quantityDelta) {
        return validateStockAdjustmentIds(productId, presentationId)
                .then(Mono.defer(() -> loadProductForStockAdjustment(
                                productId, presentationId, batchId, quantityDelta))
                        .retryWhen(OptimisticRetry.onVersionConflict()));
    }

    private Mono<Void> validateStockAdjustmentIds(String productId, String presentationId) {
        if (productId == null || productId.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requiere el producto para ajustar el stock"));
        }
        if (presentationId == null || presentationId.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requiere la presentación para ajustar el stock del producto " + productId));
        }
        return Mono.empty();
    }

    private Mono<Void> loadProductForStockAdjustment(String productId, String presentationId,
                                                     String batchId, int quantityDelta) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Producto no encontrado: " + productId)))
                .flatMap(product -> applyStockAdjustment(product, presentationId, batchId, quantityDelta));
    }

    private Mono<Void> applyStockAdjustment(Product product, String presentationId,
                                            String batchId, int quantityDelta) {
        Map<String, Object> presentation = CatalogInventorySupport.findPresentation(product, presentationId);
        Map<String, Object> batch = CatalogInventorySupport.findBatch(presentation, batchId);
        if (presentation == null || batch == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No se encontró la presentación o el lote del producto " + product.getId()));
        }
        if (wouldMakeStockNegative(presentation, batch, quantityDelta)) {
            return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
                    "El ajuste dejaría en negativo el stock de la presentación o del lote"));
        }
        return persistStockAdjustment(product, presentationId, batchId, quantityDelta);
    }

    private boolean wouldMakeStockNegative(Map<String, Object> presentation,
                                           Map<String, Object> batch, int quantityDelta) {
        return CatalogInventorySupport.presentationStock(presentation) + quantityDelta < MIN_STOCK_VALUE
                || CatalogInventorySupport.batchStock(batch) + quantityDelta < MIN_STOCK_VALUE;
    }

    private Mono<Void> persistStockAdjustment(Product product, String presentationId,
                                              String batchId, int quantityDelta) {
        if (!updatePresentationStock(product, presentationId, batchId, quantityDelta)) {
            log.warn("Presentation '{}' not found in product '{}' — stock not adjusted",
                    presentationId, product.getId());
            return Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La presentación '" + presentationId + "' no existe en el producto '"
                            + product.getId() + "'; el stock no se actualizó"));
        }
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product).then();
    }

    private boolean updatePresentationStock(Product product, String presentationId, String batchId, int quantityDelta) {
        if (product.getPresentations() == null) return false;
        for (Object presentationValue : product.getPresentations()) {
            if (!(presentationValue instanceof Map<?, ?> rawPresentation)) continue;
            Map<String, Object> presentation = CatalogInventorySupport.asStringMap(rawPresentation);
            String currentId = String.valueOf(presentation.getOrDefault(CatalogFields.ID, presentation.get(CatalogFields.ALT_ID)));
            if (presentationId.equals(currentId)) {
                updateStockInPresentation(presentation, quantityDelta);
                if (batchId != null && !batchId.isBlank()) {
                    updateStockInBatch(presentation, batchId, quantityDelta);
                }
                return true;
            }
        }
        return false;
    }

    private void updateStockInPresentation(Map<String, Object> presentation, int quantityDelta) {
        int currentStock = CatalogInventorySupport.presentationStock(presentation);
        presentation.put(CatalogFields.CURRENT_STOCK, Math.max(MIN_STOCK_VALUE, currentStock + quantityDelta));
    }

    private void updateStockInBatch(Map<String, Object> presentation, String batchId, int quantityDelta) {
        Object batchesValue = presentation.get(CatalogFields.BATCHES);
        if (!(batchesValue instanceof List<?> batches)) return;
        for (Object batchValue : batches) {
            if (!(batchValue instanceof Map<?, ?> rawBatch)) continue;
            Map<String, Object> batch = CatalogInventorySupport.asStringMap(rawBatch);
            String currentId = String.valueOf(batch.getOrDefault(CatalogFields.ID, batch.get(CatalogFields.ALT_ID)));
            if (batchId.equals(currentId)) {
                int currentStock = CatalogInventorySupport.batchStock(batch);
                batch.put(CatalogFields.BATCH_STOCK, Math.max(MIN_STOCK_VALUE, currentStock + quantityDelta));
                return;
            }
        }
    }
}
