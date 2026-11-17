package pe.edu.vallegrande.farmagro.service.impl;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
final class ProductInventoryMerger {

    // A stored presentation missing from the request is kept as it is: dropping it would erase its
    // stock and batches and break cancelling or restoring the sales and purchases that use it.
    // To retire a presentation the client sends it with status false (logical delete).
    List<Object> merge(List<Object> existing, List<Object> incomingValues) {
        Map<String, Map<String, Object>> existingById = indexById(existing);
        List<Object> merged = new ArrayList<>(incomingValues.size());
        Set<String> incomingIds = new HashSet<>();
        for (Object incomingValue : incomingValues) {
            merged.add(mergePresentation(incomingValue, existingById));
            if (incomingValue instanceof Map<?, ?> incomingMap && embeddedId(incomingMap) != null) {
                incomingIds.add(embeddedId(incomingMap));
            }
        }
        existingById.forEach((presentationId, storedPresentation) -> {
            if (!incomingIds.contains(presentationId)) {
                merged.add(storedPresentation);
            }
        });
        return merged;
    }

    private Map<String, Map<String, Object>> indexById(List<Object> presentations) {
        Map<String, Map<String, Object>> presentationsById = new LinkedHashMap<>();
        if (presentations == null) return presentationsById;
        for (Object presentationValue : presentations) {
            if (presentationValue instanceof Map<?, ?> presentationMap) {
                String presentationId = embeddedId(presentationMap);
                if (presentationId != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedPresentation = (Map<String, Object>) presentationMap;
                    presentationsById.put(presentationId, typedPresentation);
                }
            }
        }
        return presentationsById;
    }

    private Object mergePresentation(Object incomingValue, Map<String, Map<String, Object>> existingById) {
        if (!(incomingValue instanceof Map<?, ?> incomingMap)) return incomingValue;
        @SuppressWarnings("unchecked")
        Map<String, Object> mergedPresentation = new HashMap<>((Map<String, Object>) incomingMap);
        Map<String, Object> existingPresentation = existingById.get(embeddedId(incomingMap));
        if (existingPresentation != null) {
            preserveExistingInventory(existingPresentation, mergedPresentation);
        }
        return mergedPresentation;
    }

    // Stock and batches always come from storage (they only change through sales and purchases);
    // any other stored field the client did not send (shelf life, temperatures, dates...) is kept.
    private void preserveExistingInventory(Map<String, Object> existing, Map<String, Object> incoming) {
        preserveInventoryField(existing, incoming, CatalogFields.CURRENT_STOCK);
        preserveInventoryField(existing, incoming, CatalogFields.BATCHES);
        existing.forEach((field, storedValue) -> {
            if (incoming.get(field) == null && storedValue != null) {
                incoming.put(field, storedValue);
            }
        });
    }

    private String embeddedId(Map<?, ?> embeddedDocument) {
        Object id = embeddedDocument.containsKey(CatalogFields.ID)
                ? embeddedDocument.get(CatalogFields.ID) : embeddedDocument.get(CatalogFields.ALT_ID);
        return id == null ? null : String.valueOf(id);
    }

    private void preserveInventoryField(Map<String, Object> current, Map<String, Object> incoming, String field) {
        if (current.containsKey(field)) {
            incoming.put(field, current.get(field));
        } else {
            incoming.remove(field);
        }
    }
}
