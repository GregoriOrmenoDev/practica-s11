package pe.edu.vallegrande.farmagro.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductBarcodeTest {

    @Test
    @DisplayName("Presentations without barcode get a generated one; existing barcodes are kept")
    void shouldGenerateOnlyMissingBarcodes() {
        Map<String, Object> withCode = new HashMap<>(Map.of(CatalogFields.ID, "PST001", CatalogFields.BARCODE, "7750000001"));
        Map<String, Object> withoutCode = new HashMap<>(Map.of(CatalogFields.ID, "PST002"));
        Map<String, Object> blankCode = new HashMap<>(Map.of(CatalogFields.ID, "PST003", CatalogFields.BARCODE, " "));

        List<Object> result = new ProductEntityFactory().withBarcodes(List.of(withCode, withoutCode, blankCode));

        assertEquals("7750000001", ((Map<?, ?>) result.get(0)).get(CatalogFields.BARCODE));
        String generated = (String) ((Map<?, ?>) result.get(1)).get(CatalogFields.BARCODE);
        assertTrue(generated.matches("^775\\d{7}$"), generated);
        assertNotEquals(" ", ((Map<?, ?>) result.get(2)).get(CatalogFields.BARCODE));
        assertEquals("PST002", ((Map<?, ?>) result.get(1)).get(CatalogFields.ID));
    }
}
