package pe.edu.vallegrande.farmagro.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.farmagro.dto.NewBatchDTO;
import pe.edu.vallegrande.farmagro.dto.PurchaseDetailDTO;
import pe.edu.vallegrande.farmagro.model.Product;
import pe.edu.vallegrande.farmagro.repository.ProductRepository;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
final class PurchaseDetailValidator {

    private final ProductRepository productRepository;

    Mono<Void> validate(List<PurchaseDetailDTO> details) {
        return requireDetails(details)
                .thenMany(Flux.fromIterable(details))
                .concatMap(this::validateDetail)
                .then();
    }

    Mono<Void> requireDetails(List<PurchaseDetailDTO> details) {
        if (details == null || details.isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La orden de compra debe incluir al menos un detalle de producto"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateDetail(PurchaseDetailDTO detail) {
        if (detail.getMongoIdProduct() == null || detail.getMongoIdProduct().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El id del producto en el detalle es obligatorio"));
        }
        if (detail.getQuantity() == null || detail.getQuantity() < AppConstants.MINIMUM_QUANTITY) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cantidad en el detalle debe ser al menos " + AppConstants.MINIMUM_QUANTITY));
        }
        if (!hasValidSubtotal(detail)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El subtotal del detalle debe ser igual a cantidad por precio unitario"));
        }
        return productRepository.findById(detail.getMongoIdProduct())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El producto con id " + detail.getMongoIdProduct()
                                + " no existe en el catálogo")))
                .flatMap(product -> validateProduct(detail, product));
    }

    private boolean hasValidSubtotal(PurchaseDetailDTO detail) {
        BigDecimal expectedSubtotal = detail.getUnitPrice() == null ? BigDecimal.ZERO
                : detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
        return detail.getSubtotal() != null && detail.getSubtotal().compareTo(expectedSubtotal) == 0;
    }

    private Mono<Void> validateProduct(PurchaseDetailDTO detail, Product product) {
        if (Boolean.FALSE.equals(product.getStatus())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El producto " + product.getName() + " (" + detail.getMongoIdProduct()
                            + ") se encuentra inactivo"));
        }
        return validatePresentation(detail, product);
    }

    private Mono<Void> validatePresentation(PurchaseDetailDTO detail, Product product) {
        Map<String, Object> presentation = CatalogInventorySupport.findPresentation(
                product, detail.getMongoIdPresentation());
        if (presentation == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La presentación " + detail.getMongoIdPresentation()
                            + " no existe en el producto " + product.getName()));
        }
        return validateBatch(detail, presentation);
    }

    // Each line names an existing batch or brings a new one (never both). Units only go into a batch
    // that has not expired; a new batch must expire after it was manufactured and after today.
    private Mono<Void> validateBatch(PurchaseDetailDTO detail, Map<String, Object> presentation) {
        boolean hasExistingBatch = detail.getMongoIdBatch() != null && !detail.getMongoIdBatch().isBlank();
        if (hasExistingBatch == (detail.getNewBatch() != null)) {
            return badRequest("Cada línea de la compra debe indicar un lote existente o los datos de un lote nuevo (no ambos)");
        }
        return hasExistingBatch ? validateExistingBatch(detail, presentation) : validateNewBatch(detail, presentation);
    }

    private Mono<Void> validateNewBatch(PurchaseDetailDTO detail, Map<String, Object> presentation) {
        NewBatchDTO newBatch = detail.getNewBatch();
        LocalDate expires = newBatch.getExpirationDate();
        if (expires == null || newBatch.getBatchNumber() == null || newBatch.getBatchNumber().isBlank()) {
            return badRequest("El lote nuevo requiere número y fecha de vencimiento");
        }
        if (!expires.isAfter(LocalDate.now())) {
            return badRequest("El lote nuevo " + newBatch.getBatchNumber() + " ya está vencido o vence hoy");
        }
        if (newBatch.getManufactureDate() != null && expires.isBefore(newBatch.getManufactureDate())) {
            return badRequest("La fecha de vencimiento de un lote no puede ser anterior a su fecha de fabricación");
        }
        if (hasBatchNumber(presentation, newBatch.getBatchNumber())) {
            return badRequest("La presentación " + detail.getMongoIdPresentation()
                    + " ya tiene un lote con el número " + newBatch.getBatchNumber());
        }
        return Mono.empty();
    }

    private boolean hasBatchNumber(Map<String, Object> presentation, String batchNumber) {
        return presentation.get(CatalogFields.BATCHES) instanceof List<?> batches && batches.stream()
                .filter(Map.class::isInstance)
                .map(batch -> ((Map<?, ?>) batch).get(CatalogFields.BATCH_NUMBER))
                .anyMatch(number -> number != null && batchNumber.trim().equalsIgnoreCase(number.toString().trim()));
    }

    private Mono<Void> validateExistingBatch(PurchaseDetailDTO detail, Map<String, Object> presentation) {
        Map<String, Object> batch = CatalogInventorySupport.findBatch(presentation, detail.getMongoIdBatch());
        if (batch == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El lote " + detail.getMongoIdBatch()
                            + " no pertenece a la presentación " + detail.getMongoIdPresentation()));
        }
        if (Boolean.FALSE.equals(batch.get(CatalogFields.STATUS))) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El lote " + detail.getMongoIdBatch() + " se encuentra inactivo"));
        }
        if (CatalogValueReader.isExpired(batch)) {
            return badRequest("El lote " + detail.getMongoIdBatch()
                    + " está vencido: registre la compra en un lote nuevo");
        }
        return Mono.empty();
    }

    private static Mono<Void> badRequest(String message) {
        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message));
    }
}
