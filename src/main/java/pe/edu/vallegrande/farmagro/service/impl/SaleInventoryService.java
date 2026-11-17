package pe.edu.vallegrande.farmagro.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.vallegrande.farmagro.dto.SaleDetailDTO;
import pe.edu.vallegrande.farmagro.model.Product;
import pe.edu.vallegrande.farmagro.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
final class SaleInventoryService {

    private static final int MINIMUM_STOCK = 0;

    private final ProductRepository productRepository;

    Mono<Void> adjustWithRollback(List<SaleDetailDTO> saleDetails, int direction) {
        List<SaleDetailDTO> adjustedDetails = new ArrayList<>();
        return Flux.fromIterable(saleDetails)
                .concatMap(detail -> adjustProductStock(detail.getMongoIdProduct(),
                        detail.getMongoIdPresentation(), detail.getMongoIdBatch(),
                        direction * detail.getQuantity())
                        .thenReturn(detail)
                        .doOnNext(adjustedDetails::add))
                .then()
                .onErrorResume(error -> rollbackAdjustedStock(adjustedDetails, -direction, error)
                        .then(Mono.error(error)));
    }

    private Mono<Void> rollbackAdjustedStock(List<SaleDetailDTO> adjustedDetails,
                                             int direction, Throwable originalError) {
        List<SaleDetailDTO> reverseOrder = new ArrayList<>(adjustedDetails);
        Collections.reverse(reverseOrder);
        return Flux.fromIterable(reverseOrder)
                .concatMap(detail -> adjustProductStock(detail.getMongoIdProduct(),
                        detail.getMongoIdPresentation(), detail.getMongoIdBatch(),
                        direction * detail.getQuantity()))
                .onErrorContinue((rollbackError, detail) -> log.error(
                        "Failed to compensate inventory after sale stock failure; original error: {}",
                        originalError.getMessage(), rollbackError))
                .then();
    }

    private Mono<Void> adjustProductStock(String productId, String presentationId,
                                          String batchId, int quantityDelta) {
        if (productId == null || productId.isBlank()) return Mono.empty();
        return Mono.defer(() -> productRepository.findById(productId)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Producto no encontrado: " + productId)))
                        .flatMap(product -> updateProductStock(product, presentationId, batchId, quantityDelta)))
                .retryWhen(OptimisticRetry.onVersionConflict());
    }

    private Mono<Void> updateProductStock(Product product, String presentationId,
                                          String batchId, int quantityDelta) {
        return updateStockInCatalog(product, presentationId, batchId, quantityDelta)
                .then(Mono.defer(() -> {
                    product.setUpdatedAt(LocalDateTime.now());
                    return productRepository.save(product).then();
                }));
    }

    private Mono<Void> updateStockInCatalog(Product product, String presentationId,
                                            String batchId, int quantityDelta) {
        if (product.getPresentations() == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Producto sin presentaciones: " + product.getId()));
        }
        Map<String, Object> presentation = CatalogInventorySupport.findPresentation(product, presentationId);
        if (presentation == null) {
            log.warn("Presentation '{}' not found in product '{}' — stock not adjusted",
                    presentationId, product.getId());
            return Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La presentación '" + presentationId + "' no existe en el producto '"
                            + product.getId() + "'; el stock no se actualizó"));
        }
        return updatePresentationStock(presentation, batchId, quantityDelta, product.getName());
    }

    // Stock may never go negative at presentation or batch level.
    private Mono<Void> updatePresentationStock(Map<String, Object> presentation, String batchId,
                                               int quantityDelta, String productName) {
        int newStock = CatalogInventorySupport.presentationStock(presentation) + quantityDelta;
        if (newStock < MINIMUM_STOCK) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stock insuficiente para actualizar el producto " + productName));
        }
        return findAdjustableBatch(presentation, batchId, productName)
                .flatMap(batch -> applyBatchStock(batch, quantityDelta, productName)
                        .doOnSuccess(ignored -> presentation.put(CatalogFields.CURRENT_STOCK, newStock)));
    }

    private Mono<Map<String, Object>> findAdjustableBatch(Map<String, Object> presentation, String batchId,
                                                          String productName) {
        if (batchId == null || batchId.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requiere el lote para ajustar el stock del producto " + productName));
        }
        return Mono.justOrEmpty(CatalogInventorySupport.findBatch(presentation, batchId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El lote '" + batchId + "' no existe en el producto " + productName)));
    }

    private Mono<Void> applyBatchStock(Map<String, Object> batch, int quantityDelta, String productName) {
        int newBatchStock = CatalogInventorySupport.batchStock(batch) + quantityDelta;
        if (newBatchStock < MINIMUM_STOCK) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stock insuficiente en el lote " + batch.get(CatalogFields.ID) + " del producto " + productName));
        }
        batch.put(CatalogFields.BATCH_STOCK, newBatchStock);
        return Mono.empty();
    }
}
