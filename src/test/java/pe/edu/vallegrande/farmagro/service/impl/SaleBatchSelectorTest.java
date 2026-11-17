package pe.edu.vallegrande.farmagro.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaleBatchSelectorTest {

    private final SaleBatchSelector saleBatchSelector = new SaleBatchSelector();

    @Test
    @DisplayName("FEFO picks the sellable batch that expires first")
    void shouldPickEarliestExpiringSellableBatch() {
        Map<String, Object> presentation = presentation(
                batch("LOT001", LocalDate.now().plusYears(2), 50),
                batch("LOT002", LocalDate.now().plusMonths(3), 50),
                batch("LOT003", LocalDate.now().minusDays(1), 50));

        assertEquals("LOT002", saleBatchSelector.selectBatchId(presentation, 10, batchId -> 0));
    }

    @Test
    @DisplayName("Falls back to the first batch when none can cover the quantity")
    void shouldFallBackToFirstBatchWhenStockIsShort() {
        Map<String, Object> presentation = presentation(
                batch("LOT001", LocalDate.now().plusYears(1), 2),
                batch("LOT002", LocalDate.now().plusMonths(6), 3));

        assertEquals("LOT001", saleBatchSelector.selectBatchId(presentation, 10, batchId -> 0));
    }

    private Map<String, Object> presentation(Map<String, Object>... batches) {
        return Map.of(CatalogFields.BATCHES, List.of(batches));
    }

    private Map<String, Object> batch(String batchId, LocalDate expirationDate, int stock) {
        return Map.of(CatalogFields.ID, batchId,
                CatalogFields.EXPIRATION_DATE, expirationDate,
                CatalogFields.BATCH_STOCK, stock);
    }
}
