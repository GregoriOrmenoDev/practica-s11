package pe.edu.vallegrande.farmagro.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.vallegrande.farmagro.dto.NewBatchDTO;
import pe.edu.vallegrande.farmagro.dto.PurchaseDetailDTO;
import pe.edu.vallegrande.farmagro.model.Product;
import pe.edu.vallegrande.farmagro.repository.ProductRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PurchaseNewBatchTest {

    private final ProductRepository repository = mock(ProductRepository.class);

    @Test
    @DisplayName("A purchase line with a new batch creates it empty and points the line to it")
    void shouldCreateEmptyBatchForLine() {
        Product product = product(LocalDate.now().plusYears(1));
        when(repository.findById("PRD900")).thenReturn(Mono.just(product));
        when(repository.save(any(Product.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));
        PurchaseDetailDTO line = line(null, newBatch("L-2027", LocalDate.now().plusYears(2)));

        StepVerifier.create(new PurchaseBatchService(repository, new ProductEntityFactory()).createNewBatches(List.of(line)))
                .assertNext(created -> assertEquals(1, created.size()))
                .verifyComplete();

        Map<String, Object> batch = CatalogInventorySupport.findBatch(
                CatalogInventorySupport.findPresentation(product, "PST900"), line.getMongoIdBatch());
        assertTrue(line.getMongoIdBatch().matches("^LOT\\d{7}$"), line.getMongoIdBatch());
        assertEquals(0, batch.get(CatalogFields.BATCH_STOCK));
        assertEquals("L-2027", batch.get(CatalogFields.BATCH_NUMBER));
    }

    @Test
    @DisplayName("Units cannot go into an expired batch, and a line needs exactly one kind of batch")
    void shouldRejectExpiredBatchAndAmbiguousLines() {
        when(repository.findById("PRD900")).thenReturn(Mono.just(product(LocalDate.now().minusDays(1))));
        PurchaseDetailValidator validator = new PurchaseDetailValidator(repository);

        StepVerifier.create(validator.validate(List.of(line("LOT900", null))))
                .expectErrorSatisfies(error -> assertTrue(reason(error).contains("vencido")))
                .verify();
        StepVerifier.create(validator.validate(List.of(line("LOT900", newBatch("L-1", LocalDate.now().plusYears(1))))))
                .expectErrorSatisfies(error -> assertTrue(reason(error).contains("no ambos")))
                .verify();
        StepVerifier.create(validator.validate(List.of(line(null, newBatch("OLD-LOT", LocalDate.now().plusYears(1))))))
                .expectErrorSatisfies(error -> assertTrue(reason(error).contains("ya tiene un lote")))
                .verify();
        StepVerifier.create(validator.validate(List.of(line(null, newBatch("L-2", LocalDate.now().plusYears(1))))))
                .verifyComplete();
    }

    @Test
    @DisplayName("If the purchase fails after creating its batch, the still-empty batch is removed")
    void shouldRemoveEmptyBatchOnCompensation() {
        Product product = product(LocalDate.now().plusYears(1));
        Map<String, Object> presentation = CatalogInventorySupport.findPresentation(product, "PST900");
        @SuppressWarnings("unchecked")
        List<Object> batches = (List<Object>) presentation.get(CatalogFields.BATCHES);
        batches.add(new HashMap<>(Map.of(CatalogFields.ID, "LOT901", CatalogFields.BATCH_STOCK, 0)));
        when(repository.findById("PRD900")).thenReturn(Mono.just(product));
        when(repository.save(any(Product.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(new PurchaseBatchService(repository, new ProductEntityFactory())
                        .removeBatches(List.of(new PurchaseBatchService.CreatedBatch("PRD900", "PST900", "LOT901"))))
                .verifyComplete();

        assertEquals(null, CatalogInventorySupport.findBatch(CatalogInventorySupport.findPresentation(product, "PST900"), "LOT901"));
        assertEquals(3, CatalogInventorySupport.findBatch(CatalogInventorySupport.findPresentation(product, "PST900"), "LOT900")
                .get(CatalogFields.BATCH_STOCK));
    }

    private String reason(Throwable error) {
        return ((ResponseStatusException) error).getReason();
    }

    private NewBatchDTO newBatch(String number, LocalDate expiration) {
        return NewBatchDTO.builder().batchNumber(number).manufactureDate(LocalDate.now().minusMonths(1)).expirationDate(expiration).build();
    }

    private PurchaseDetailDTO line(String batchId, NewBatchDTO newBatch) {
        return PurchaseDetailDTO.builder().quantity(5).unitPrice(new BigDecimal("10.00")).subtotal(new BigDecimal("50.00"))
                .mongoIdProduct("PRD900").mongoIdPresentation("PST900").mongoIdBatch(batchId).newBatch(newBatch).build();
    }

    private Product product(LocalDate existingBatchExpiration) {
        Map<String, Object> batch = new HashMap<>(Map.of(CatalogFields.ID, "LOT900", CatalogFields.BATCH_NUMBER, "OLD-LOT",
                CatalogFields.BATCH_STOCK, 3, CatalogFields.STATUS, true, CatalogFields.EXPIRATION_DATE, existingBatchExpiration));
        Map<String, Object> presentation = new HashMap<>(Map.of(CatalogFields.ID, "PST900", CatalogFields.CURRENT_STOCK, 3,
                CatalogFields.BATCHES, new ArrayList<>(List.of(batch))));
        return Product.builder().id("PRD900").name("Test").status(true).presentations(new ArrayList<>(List.of(presentation))).build();
    }
}
