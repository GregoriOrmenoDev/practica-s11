package pe.edu.vallegrande.farmagro.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.farmagro.dto.SaleDetailDTO;
import pe.edu.vallegrande.farmagro.model.Product;
import pe.edu.vallegrande.farmagro.repository.ProductRepository;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
final class SaleDetailValidator {

    private record CatalogSelection(Product product, String presentationId, Map<String, Object> presentation) {
    }

    private record SaleDetailContext(
            SaleDetailDTO detail,
            CatalogSelection selection,
            Map<String, Integer> requestedTotals) {

        Product product() {
            return selection.product();
        }

        String presentationId() {
            return selection.presentationId();
        }

        Map<String, Object> presentation() {
            return selection.presentation();
        }
    }

    private static final BigDecimal MINIMUM_PRICE = BigDecimal.ZERO;

    private final ProductRepository productRepository;
    private final SaleBatchSelector saleBatchSelector;

    Mono<Void> validate(List<SaleDetailDTO> details) {
        return validateDetailsExist(details)
                .then(Mono.defer(() -> validateNormalizedDetails(details)));
    }

    private Mono<Void> validateDetailsExist(List<SaleDetailDTO> details) {
        if (details == null || details.isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La venta debe incluir al menos un detalle de producto"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateNormalizedDetails(List<SaleDetailDTO> details) {
        normalizeItemDiscounts(details);
        Map<String, Integer> requestedTotals = buildRequestedTotals(details);
        return Flux.fromIterable(details)
                .concatMap(detail -> validateSaleDetail(detail, requestedTotals))
                .then();
    }

    private void normalizeItemDiscounts(List<SaleDetailDTO> details) {
        details.forEach(detail -> {
            if (detail.getItemDiscount() == null) {
                detail.setItemDiscount(BigDecimal.ZERO);
            }
        });
    }

    private Map<String, Integer> buildRequestedTotals(List<SaleDetailDTO> details) {
        Map<String, Integer> requestedTotals = new HashMap<>();
        for (SaleDetailDTO detail : details) {
            addRequestedQuantity(requestedTotals, detail);
        }
        return requestedTotals;
    }

    private void addRequestedQuantity(Map<String, Integer> requestedTotals, SaleDetailDTO detail) {
        if (detail.getQuantity() == null || detail.getQuantity() < AppConstants.MINIMUM_QUANTITY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cantidad solicitada debe ser al menos " + AppConstants.MINIMUM_QUANTITY);
        }
        String productPresentationKey = (detail.getMongoIdProduct() != null ? detail.getMongoIdProduct() : "")
                + AppConstants.INVENTORY_KEY_SEPARATOR
                + (detail.getMongoIdPresentation() != null ? detail.getMongoIdPresentation() : "");
        mergeRequestedQuantity(requestedTotals, productPresentationKey, detail.getQuantity());
        // Lines without a batch are counted per batch once the backend assigns it (autoAssignBatch).
        if (detail.getMongoIdBatch() != null && !detail.getMongoIdBatch().isBlank()) {
            mergeRequestedQuantity(requestedTotals,
                    productPresentationKey + AppConstants.INVENTORY_KEY_SEPARATOR + detail.getMongoIdBatch(),
                    detail.getQuantity());
        }
    }

    private void mergeRequestedQuantity(Map<String, Integer> requestedTotals, String key, int quantity) {
        try {
            requestedTotals.merge(key, quantity, Math::addExact);
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad total solicitada excede el límite permitido");
        }
    }

    private Mono<Void> validateSaleDetail(SaleDetailDTO detail, Map<String, Integer> requestedTotals) {
        if (detail.getMongoIdProduct() == null || detail.getMongoIdProduct().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El id del producto en el detalle es obligatorio"));
        }
        return productRepository.findById(detail.getMongoIdProduct())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El producto con ID " + detail.getMongoIdProduct() + " no existe en el catálogo de MongoDB")))
                .flatMap(product -> validateDetailProduct(detail, product, requestedTotals));
    }

    private Mono<Void> validateDetailProduct(SaleDetailDTO detail, Product product,
                                             Map<String, Integer> requestedTotals) {
        return validateProductActive(detail, product)
                .then(Mono.defer(() -> selectPresentation(detail, product)))
                .flatMap(selection -> validateAgainstCatalog(new SaleDetailContext(detail, selection, requestedTotals)));
    }

    private Mono<Void> validateProductActive(SaleDetailDTO detail, Product product) {
        if (Boolean.FALSE.equals(product.getStatus())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El producto " + product.getName() + " (" + detail.getMongoIdProduct() + ") se encuentra inactivo"));
        }
        return Mono.empty();
    }

    private Mono<CatalogSelection> selectPresentation(SaleDetailDTO detail, Product product) {
        String presentationId = resolvePresentationId(detail, product);
        Map<String, Object> presentation = CatalogInventorySupport.findPresentation(product, presentationId);
        if (presentation == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La presentación " + presentationId + " no existe en el producto " + product.getName()));
        }
        if (Boolean.FALSE.equals(presentation.get(CatalogFields.STATUS))) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La presentación " + presentationId + " del producto " + product.getName() + " está inactiva"));
        }
        return Mono.just(new CatalogSelection(product, presentationId, presentation));
    }

    private Mono<Void> validateAgainstCatalog(SaleDetailContext context) {
        return validateStock(context)
                .then(Mono.defer(() -> validateBatch(context)))
                .then(Mono.defer(() -> validatePrice(context)));
    }

    private String resolvePresentationId(SaleDetailDTO detail, Product product) {
        String presentationId = detail.getMongoIdPresentation();
        if (presentationId == null || presentationId.isBlank()) {
            presentationId = findFirstPresentationId(product);
            if (presentationId != null) {
                detail.setMongoIdPresentation(presentationId);
            }
        }
        return presentationId;
    }

    private Mono<Void> validateStock(SaleDetailContext context) {
        SaleDetailDTO detail = context.detail();
        int currentStock = CatalogInventorySupport.presentationStock(context.presentation());
        String inventoryKey = detail.getMongoIdProduct() + AppConstants.INVENTORY_KEY_SEPARATOR
                + (detail.getMongoIdPresentation() != null ? detail.getMongoIdPresentation() : "");
        int totalRequested = context.requestedTotals().getOrDefault(inventoryKey, detail.getQuantity());
        if (currentStock < totalRequested) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stock insuficiente para el producto '" + context.product().getName() +
                    "'. Disponible: " + currentStock + ", solicitado: " + totalRequested));
        }
        return Mono.empty();
    }

    private Mono<Void> validateBatch(SaleDetailContext context) {
        if (context.detail().getMongoIdBatch() != null && !context.detail().getMongoIdBatch().isBlank()) {
            return validateExistingBatch(context);
        } else {
            return autoAssignBatch(context);
        }
    }

    private Mono<Void> validateExistingBatch(SaleDetailContext context) {
        Map<String, Object> batch = CatalogInventorySupport.findBatch(
                context.presentation(), context.detail().getMongoIdBatch());
        if (batch == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "El lote "
                    + context.detail().getMongoIdBatch() + " no existe en la presentación " + context.presentationId()));
        }
        return validateBatchSellable(context, batch).then(validateBatchStock(context, batch));
    }

    private Mono<Void> validateBatchSellable(SaleDetailContext context, Map<String, Object> batch) {
        String batchLabel = "El lote " + context.detail().getMongoIdBatch()
                + " del producto '" + context.product().getName() + "'";
        if (CatalogValueReader.isExpired(batch)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, batchLabel + " se encuentra vencido"));
        }
        if (Boolean.FALSE.equals(batch.get(CatalogFields.STATUS))) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, batchLabel + " se encuentra inactivo"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateBatchStock(SaleDetailContext context, Map<String, Object> batch) {
        SaleDetailDTO detail = context.detail();
        int batchStock = CatalogInventorySupport.batchStock(batch);
        String batchKey = batchKey(context, detail.getMongoIdBatch());
        int requestedBatchStock = context.requestedTotals().getOrDefault(batchKey, detail.getQuantity());
        if (batchStock < requestedBatchStock) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stock insuficiente en el lote " + detail.getMongoIdBatch() + " del producto '" +
                    context.product().getName() + "'. Stock del lote: " + batchStock + ", solicitado: " + requestedBatchStock));
        }
        return Mono.empty();
    }

    // FEFO: the earliest-expiring active batch whose stock, minus what earlier lines of this sale
    // already took, covers the line. If none qualifies, the fallback batch goes through the same
    // checks so an expired or short batch is reported, never sold.
    private Mono<Void> autoAssignBatch(SaleDetailContext context) {
        SaleDetailDTO detail = context.detail();
        Map<String, Integer> requestedTotals = context.requestedTotals();
        String selectedBatchId = saleBatchSelector.selectBatchId(context.presentation(), detail.getQuantity(),
                batchId -> requestedTotals.getOrDefault(batchKey(context, batchId), 0));
        if (selectedBatchId == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La presentación " + context.presentationId() + " del producto '" + context.product().getName()
                            + "' no tiene lotes registrados"));
        }
        detail.setMongoIdBatch(selectedBatchId);
        mergeRequestedQuantity(requestedTotals, batchKey(context, selectedBatchId), detail.getQuantity());
        return validateExistingBatch(context);
    }

    private String batchKey(SaleDetailContext context, String batchId) {
        return context.detail().getMongoIdProduct() + AppConstants.INVENTORY_KEY_SEPARATOR + context.presentationId()
                + AppConstants.INVENTORY_KEY_SEPARATOR + batchId;
    }

    private Mono<Void> validatePrice(SaleDetailContext context) {
        SaleDetailDTO detail = context.detail();
        BigDecimal catalogPrice = CatalogValueReader.salePrice(context.presentation());
        if (catalogPrice != null && catalogPrice.compareTo(MINIMUM_PRICE) > 0) {
            if (detail.getUnitPrice() == null || detail.getUnitPrice().compareTo(MINIMUM_PRICE) <= 0) {
                detail.setUnitPrice(catalogPrice);
            } else if (detail.getUnitPrice().subtract(catalogPrice).abs().compareTo(AppConstants.PRICE_TOLERANCE) > 0) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El precio unitario (S/ " + detail.getUnitPrice() + ") para el producto '" + context.product().getName() +
                        "' no coincide con el precio oficial de lista en catálogo (S/ " + catalogPrice +
                        "). Para otorgar rebajas comerciales, ingrese el descuento en 'itemDiscount'."));
            }
        }
        return Mono.empty();
    }

    private String findFirstPresentationId(Product product) {
        List<?> presentations = product.getPresentations();
        if (presentations == null || presentations.isEmpty()) return null;
        return CatalogValueReader.entryId(presentations.get(AppConstants.FIRST_ELEMENT_INDEX));
    }
}
