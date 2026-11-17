package pe.edu.vallegrande.farmagro.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.vallegrande.farmagro.dto.SaleDetailDTO;
import pe.edu.vallegrande.farmagro.model.Product;
import pe.edu.vallegrande.farmagro.repository.ProductRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaleDetailValidatorTest {

    private static final String PRODUCT_ID = "PRD900";
    private static final String PRESENTATION_ID = "PST900";

    private ProductRepository productRepository;
    private SaleDetailValidator validator;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        validator = new SaleDetailValidator(productRepository, new SaleBatchSelector());
    }

    @Test
    @DisplayName("A line without batch gets the first-expiring valid batch and skips the expired one")
    void shouldAssignFirstExpiringValidBatch() {
        givenProduct(presentation(true,
                batch("LOT901", LocalDate.now().minusDays(1), 50),
                batch("LOT902", LocalDate.now().plusYears(1), 50),
                batch("LOT903", LocalDate.now().plusMonths(2), 50)));
        SaleDetailDTO line = line(5);

        StepVerifier.create(validator.validate(List.of(line))).verifyComplete();

        assertEquals("LOT903", line.getMongoIdBatch());
    }

    @Test
    @DisplayName("Two lines of the same presentation never share a batch beyond its stock")
    void shouldCountEarlierLinesWhenAssigningBatches() {
        givenProduct(presentation(true,
                batch("LOT901", LocalDate.now().plusMonths(1), 6),
                batch("LOT902", LocalDate.now().plusMonths(6), 10)));
        SaleDetailDTO first = line(4);
        SaleDetailDTO second = line(4);

        StepVerifier.create(validator.validate(List.of(first, second))).verifyComplete();

        assertEquals("LOT901", first.getMongoIdBatch());
        assertEquals("LOT902", second.getMongoIdBatch());
    }

    @Test
    @DisplayName("Only expired batches: the sale is rejected and the expired batch is never sold")
    void shouldRejectWhenOnlyExpiredBatchesRemain() {
        givenProduct(presentation(true, batch("LOT901", LocalDate.now().minusDays(3), 50)));

        StepVerifier.create(validator.validate(List.of(line(1))))
                .expectErrorSatisfies(error -> assertBadRequest(error, "vencido"))
                .verify();
    }

    @Test
    @DisplayName("An inactive presentation cannot be sold")
    void shouldRejectInactivePresentation() {
        givenProduct(presentation(false, batch("LOT901", LocalDate.now().plusYears(1), 50)));

        StepVerifier.create(validator.validate(List.of(line(1))))
                .expectErrorSatisfies(error -> assertBadRequest(error, "inactiva"))
                .verify();
    }

    private void assertBadRequest(Throwable error, String expectedText) {
        assertTrue(error instanceof ResponseStatusException);
        ResponseStatusException exception = (ResponseStatusException) error;
        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains(expectedText), exception.getReason());
    }

    private void givenProduct(Map<String, Object> presentation) {
        Product product = Product.builder()
                .id(PRODUCT_ID)
                .name("Test fertilizer")
                .status(true)
                .presentations(List.of(presentation))
                .build();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Mono.just(product));
    }

    private SaleDetailDTO line(int quantity) {
        SaleDetailDTO detail = new SaleDetailDTO();
        detail.setMongoIdProduct(PRODUCT_ID);
        detail.setMongoIdPresentation(PRESENTATION_ID);
        detail.setQuantity(quantity);
        detail.setUnitPrice(new BigDecimal("10.00"));
        return detail;
    }

    @SafeVarargs
    private Map<String, Object> presentation(boolean active, Map<String, Object>... batches) {
        Map<String, Object> presentation = new HashMap<>();
        presentation.put(CatalogFields.ID, PRESENTATION_ID);
        presentation.put(CatalogFields.STATUS, active);
        presentation.put(CatalogFields.SALE_PRICE, 10.0);
        presentation.put(CatalogFields.CURRENT_STOCK, List.of(batches).stream()
                .mapToInt(batch -> (Integer) batch.get(CatalogFields.BATCH_STOCK)).sum());
        presentation.put(CatalogFields.BATCHES, List.of(batches));
        return presentation;
    }

    private Map<String, Object> batch(String batchId, LocalDate expirationDate, int stock) {
        Map<String, Object> batch = new HashMap<>();
        batch.put(CatalogFields.ID, batchId);
        batch.put(CatalogFields.EXPIRATION_DATE, expirationDate);
        batch.put(CatalogFields.BATCH_STOCK, stock);
        batch.put(CatalogFields.STATUS, true);
        return batch;
    }
}
