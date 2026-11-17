package pe.edu.vallegrande.farmagro.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.farmagro.dto.ProductDTO;
import pe.edu.vallegrande.farmagro.repository.SupplierRepository;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
final class ProductPresentationValidator {

    private static final int MINIMUM_STOCK = 0;
    private static final int EMPTY_STOCK_TOTAL = 0;
    private static final double MINIMUM_PRICE = 0;

    private final SupplierRepository supplierRepository;

    Mono<Void> validate(ProductDTO dto) {
        List<Object> presentations = dto.getPresentations();
        if (presentations == null || presentations.isEmpty()) {
            if (dto.getPrice() == null || !Double.isFinite(dto.getPrice()) || dto.getPrice() <= MINIMUM_PRICE) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El producto debe incluir al menos una presentación o un precio para generar la presentación estándar"));
            }
            return validateActiveSupplier(AppConstants.DEFAULT_SUPPLIER_ID);
        }
        return validatePresentations(presentations, Map.of());
    }

    // On update the supplier of a presentation is checked only when it is new or changes: a
    // supplier deactivated later must not block editing the product (price, label, status...).
    Mono<Void> validateIfProvided(ProductDTO dto, List<Object> storedPresentations) {
        return dto.getPresentations() == null || dto.getPresentations().isEmpty()
                ? Mono.empty() : validatePresentations(dto.getPresentations(), suppliersById(storedPresentations));
    }

    private Mono<Void> validatePresentations(List<Object> presentations, Map<String, Object> storedSuppliers) {
        return Flux.fromIterable(presentations)
                .concatMap(presentation -> validatePresentationValue(presentation, storedSuppliers))
                .then();
    }

    private Map<String, Object> suppliersById(List<Object> storedPresentations) {
        Map<String, Object> suppliers = new HashMap<>();
        if (storedPresentations == null) return suppliers;
        for (Object storedValue : storedPresentations) {
            if (storedValue instanceof Map<?, ?> stored && presentationId(stored) != null) {
                suppliers.put(presentationId(stored), stored.get(CatalogFields.SUPPLIER_ID));
            }
        }
        return suppliers;
    }

    private Mono<Void> validatePresentationValue(Object presentationValue, Map<String, Object> storedSuppliers) {
        if (!(presentationValue instanceof Map<?, ?> presentation)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cada presentación debe ser un objeto"));
        }
        return validatePresentation(presentation, storedSuppliers);
    }

    private Mono<Void> validatePresentation(Map<?, ?> presentation, Map<String, Object> storedSuppliers) {
        if (!hasValidPresentationId(presentation)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El ID de presentación debe usar el formato PST seguido de 3 a 7 dígitos"));
        }
        Object currentStock = presentation.get(CatalogFields.CURRENT_STOCK);
        if (!hasValidPriceAndStock(presentation, currentStock)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cada presentación requiere precio de venta positivo y stock no negativo"));
        }
        Number stock = (Number) currentStock;
        if (!hasValidBatches(presentation, stock.intValue())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los lotes deben tener IDs LOT válidos, stock no negativo y sumar el stock de la presentación"));
        }
        if (hasBatchExpiringBeforeManufacture(presentation)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de vencimiento de un lote no puede ser anterior a su fecha de fabricación"));
        }
        String presentationId = presentationId(presentation);
        boolean sameSupplier = storedSuppliers.containsKey(presentationId)
                && Objects.equals(storedSuppliers.get(presentationId), presentation.get(CatalogFields.SUPPLIER_ID));
        return sameSupplier ? Mono.empty() : validatePresentationSupplier(presentation);
    }

    private boolean hasValidPresentationId(Map<?, ?> presentation) {
        String presentationId = presentationId(presentation);
        return presentationId != null && presentationId.matches(AppConstants.PRESENTATION_ID_PATTERN);
    }

    private String presentationId(Map<?, ?> presentation) {
        Object presentationId = presentation.get(CatalogFields.ID);
        if (presentationId == null) presentationId = presentation.get(CatalogFields.ALT_ID);
        return presentationId == null ? null : presentationId.toString();
    }

    private boolean hasValidPriceAndStock(Map<?, ?> presentation, Object currentStock) {
        Object salePrice = presentation.get(CatalogFields.SALE_PRICE);
        if (!(salePrice instanceof Number price) || price.doubleValue() <= MINIMUM_PRICE) return false;
        return currentStock instanceof Number stock && stock.intValue() >= MINIMUM_STOCK;
    }

    private Mono<Void> validatePresentationSupplier(Map<?, ?> presentation) {
        Object supplierId = presentation.get(CatalogFields.SUPPLIER_ID);
        if (supplierId == null || supplierId.toString().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cada presentación debe referenciar un proveedor"));
        }
        return validateActiveSupplier(supplierId.toString());
    }

    private boolean hasValidBatches(Map<?, ?> presentation, int presentationStock) {
        Object batchesValue = presentation.get(CatalogFields.BATCHES);
        if (!(batchesValue instanceof List<?> batches) || batches.isEmpty()) return false;
        int totalBatchStock = EMPTY_STOCK_TOTAL;
        for (Object batchValue : batches) {
            if (!(batchValue instanceof Map<?, ?> batch) || !hasValidBatch(batch)) return false;
            totalBatchStock += ((Number) batch.get(CatalogFields.BATCH_STOCK)).intValue();
        }
        return totalBatchStock == presentationStock;
    }

    private boolean hasBatchExpiringBeforeManufacture(Map<?, ?> presentation) {
        if (!(presentation.get(CatalogFields.BATCHES) instanceof List<?> batches)) return false;
        for (Object batchValue : batches) {
            if (batchValue instanceof Map<?, ?> rawBatch) {
                Map<String, Object> batch = CatalogInventorySupport.asStringMap(rawBatch);
                LocalDate manufactured = CatalogValueReader.manufactureDate(batch);
                LocalDate expires = CatalogValueReader.expirationDate(batch);
                if (manufactured != null && expires != null && expires.isBefore(manufactured)) return true;
            }
        }
        return false;
    }

    private boolean hasValidBatch(Map<?, ?> batch) {
        Object batchId = batch.get(CatalogFields.ID);
        if (batchId == null) batchId = batch.get(CatalogFields.ALT_ID);
        Object batchStock = batch.get(CatalogFields.BATCH_STOCK);
        return batchId != null && batchId.toString().matches(AppConstants.BATCH_ID_PATTERN)
                && batchStock instanceof Number stock && stock.intValue() >= MINIMUM_STOCK;
    }

    private Mono<Void> validateActiveSupplier(String supplierId) {
        return supplierRepository.findById(supplierId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El proveedor " + supplierId + " no existe")))
                .flatMap(supplier -> Boolean.FALSE.equals(supplier.getStatus())
                        ? Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El proveedor " + supplierId + " está inactivo"))
                        : Mono.empty());
    }
}
