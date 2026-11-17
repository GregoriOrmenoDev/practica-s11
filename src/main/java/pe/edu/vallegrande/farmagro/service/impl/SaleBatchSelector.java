package pe.edu.vallegrande.farmagro.service.impl;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;

@Component
final class SaleBatchSelector {

    // FEFO first; else the first batch, so the stock check reports the gap.
    // reservedUnits: units of each batch already taken by earlier lines of the same sale.
    String selectBatchId(Map<String, Object> presentation, int requestedQuantity,
                         ToIntFunction<String> reservedUnits) {
        String fefoBatchId = findFefoBatchId(batchesOf(presentation), requestedQuantity, reservedUnits);
        return fefoBatchId != null ? fefoBatchId : firstBatchId(batchesOf(presentation));
    }

    private String findFefoBatchId(List<?> batches, int requestedQuantity, ToIntFunction<String> reservedUnits) {
        String selectedBatchId = null;
        LocalDate selectedExpirationDate = null;
        for (Object batchEntry : batches) {
            Map<String, Object> batch = asSellableBatch(batchEntry, requestedQuantity, reservedUnits);
            if (batch == null) {
                continue;
            }
            LocalDate expirationDate = CatalogValueReader.expirationDate(batch);
            if (isEarlierBatch(expirationDate, selectedExpirationDate, selectedBatchId)) {
                selectedExpirationDate = expirationDate;
                selectedBatchId = CatalogValueReader.entryId(batch);
            }
        }
        return selectedBatchId;
    }

    private String firstBatchId(List<?> batches) {
        return batches.stream()
                .map(CatalogValueReader::entryId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<?> batchesOf(Map<String, Object> presentation) {
        return presentation != null && presentation.get(CatalogFields.BATCHES) instanceof List<?> batches
                ? batches : List.of();
    }

    private Map<String, Object> asSellableBatch(Object batchEntry, int requestedQuantity,
                                                ToIntFunction<String> reservedUnits) {
        if (!(batchEntry instanceof Map<?, ?> rawBatch)) {
            return null;
        }
        Map<String, Object> batch = CatalogInventorySupport.asStringMap(rawBatch);
        return isSellable(batch, requestedQuantity, reservedUnits) ? batch : null;
    }

    private boolean isSellable(Map<String, Object> batch, int requestedQuantity, ToIntFunction<String> reservedUnits) {
        boolean active = !Boolean.FALSE.equals(batch.getOrDefault(CatalogFields.STATUS, true));
        String batchId = CatalogValueReader.entryId(batch);
        int freeStock = CatalogInventorySupport.batchStock(batch) - (batchId == null ? 0 : reservedUnits.applyAsInt(batchId));
        return active && !CatalogValueReader.isExpired(batch) && freeStock >= requestedQuantity;
    }

    private boolean isEarlierBatch(LocalDate candidateDate, LocalDate selectedDate, String selectedBatchId) {
        return selectedBatchId == null || (candidateDate != null
                && (selectedDate == null || candidateDate.isBefore(selectedDate)));
    }
}
