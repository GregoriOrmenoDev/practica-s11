package pe.edu.vallegrande.farmagro.service.impl;

import lombok.extern.slf4j.Slf4j;
import pe.edu.vallegrande.farmagro.model.Product;

import java.util.List;
import java.util.Map;

@Slf4j
final class CatalogInventorySupport {

    private static final int UNREADABLE_STOCK = 0;

    private CatalogInventorySupport() {
    }

    static Map<String, Object> findPresentation(Product product, String presentationId) {
        return findEntryById(product.getPresentations(), presentationId);
    }

    static Map<String, Object> findBatch(Map<String, Object> presentation, String batchId) {
        if (presentation == null) return null;
        return presentation.get(CatalogFields.BATCHES) instanceof List<?> batches
                ? findEntryById(batches, batchId) : null;
    }

    private static Map<String, Object> findEntryById(List<?> entries, String entryId) {
        if (entries == null || entryId == null) return null;
        for (Object entry : entries) {
            if (entry instanceof Map<?, ?> entryMap && entryId.equals(String.valueOf(entryIdOf(entryMap)))) {
                return asStringMap(entryMap);
            }
        }
        return null;
    }

    private static Object entryIdOf(Map<?, ?> entryMap) {
        return entryMap.containsKey(CatalogFields.ID)
                ? entryMap.get(CatalogFields.ID) : entryMap.get(CatalogFields.ALT_ID);
    }

    static int presentationStock(Map<String, Object> presentation) {
        Object stock = presentation == null ? null : presentation.get(CatalogFields.CURRENT_STOCK);
        return extractStock(stock, "presentation");
    }

    static int batchStock(Map<String, Object> batch) {
        Object stock = batch == null ? null : batch.get(CatalogFields.BATCH_STOCK);
        return extractStock(stock, "batch");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asStringMap(Map<?, ?> catalogEntry) {
        return (Map<String, Object>) catalogEntry;
    }

    private static int extractStock(Object stockValue, String stockKind) {
        if (stockValue instanceof Number number) return number.intValue();
        if (stockValue instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException exception) {
                log.warn("Error parsing {} stock: {}", stockKind, text);
            }
        }
        return UNREADABLE_STOCK;
    }
}
