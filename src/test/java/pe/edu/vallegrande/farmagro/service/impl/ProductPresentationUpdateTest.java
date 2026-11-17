package pe.edu.vallegrande.farmagro.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.vallegrande.farmagro.dto.ProductDTO;
import pe.edu.vallegrande.farmagro.model.Supplier;
import pe.edu.vallegrande.farmagro.repository.SupplierRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductPresentationUpdateTest {

    private final SupplierRepository supplierRepository = mock(SupplierRepository.class);
    private final ProductPresentationValidator validator = new ProductPresentationValidator(supplierRepository);

    @Test
    @DisplayName("A presentation missing from the update keeps its stock and batches")
    void mergeShouldKeepOmittedPresentation() {
        Map<String, Object> first = presentation("PST001", "SUP001", 10);
        Map<String, Object> second = presentation("PST002", "SUP001", 7);
        Map<String, Object> editedFirst = new HashMap<>(Map.of(CatalogFields.ID, "PST001", CatalogFields.SALE_PRICE, 25.0));

        List<Object> merged = new ProductInventoryMerger().merge(List.of(first, second), List.of(editedFirst));

        assertEquals(2, merged.size());
        assertEquals(second, merged.get(1));
        assertEquals(25.0, ((Map<?, ?>) merged.get(0)).get(CatalogFields.SALE_PRICE));
        assertEquals(10, ((Map<?, ?>) merged.get(0)).get(CatalogFields.CURRENT_STOCK));
    }

    @Test
    @DisplayName("Editing a product does not re-check the unchanged supplier of a presentation")
    void updateShouldSkipUnchangedSupplier() {
        Map<String, Object> stored = presentation("PST001", "SUP009", 10);
        Map<String, Object> edited = new HashMap<>(stored);
        edited.put(CatalogFields.SALE_PRICE, 30.0);

        StepVerifier.create(validator.validateIfProvided(product(edited), List.of(stored))).verifyComplete();

        verify(supplierRepository, never()).findById("SUP009");
    }

    @Test
    @DisplayName("Switching a presentation to an inactive supplier is rejected")
    void updateShouldValidateChangedSupplier() {
        Map<String, Object> stored = presentation("PST001", "SUP001", 10);
        Map<String, Object> edited = new HashMap<>(stored);
        edited.put(CatalogFields.SUPPLIER_ID, "SUP009");
        when(supplierRepository.findById("SUP009"))
                .thenReturn(Mono.just(Supplier.builder().id("SUP009").status(false).build()));

        StepVerifier.create(validator.validateIfProvided(product(edited), List.of(stored)))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof ResponseStatusException);
                    assertTrue(((ResponseStatusException) error).getReason().contains("inactivo"));
                })
                .verify();
    }

    @Test
    @DisplayName("A batch that expires before it was manufactured is rejected")
    void shouldRejectBatchExpiringBeforeManufacture() {
        Map<String, Object> presentation = presentation("PST002", "SUP001", 4);
        presentation.put(CatalogFields.BATCHES, List.of(Map.of(CatalogFields.ID, "LOT002", CatalogFields.BATCH_STOCK, 4,
                CatalogFields.MANUFACTURE_DATE, "2026-05-01", CatalogFields.EXPIRATION_DATE, "2026-01-01")));

        StepVerifier.create(validator.validateIfProvided(product(presentation), List.of()))
                .expectErrorSatisfies(error -> assertTrue(((ResponseStatusException) error).getReason().contains("vencimiento")))
                .verify();
    }

    private ProductDTO product(Map<String, Object> presentation) {
        ProductDTO dto = new ProductDTO();
        dto.setPresentations(List.of(presentation));
        return dto;
    }

    private Map<String, Object> presentation(String id, String supplierId, int stock) {
        Map<String, Object> presentation = new HashMap<>();
        presentation.put(CatalogFields.ID, id);
        presentation.put(CatalogFields.SALE_PRICE, 20.0);
        presentation.put(CatalogFields.CURRENT_STOCK, stock);
        presentation.put(CatalogFields.SUPPLIER_ID, supplierId);
        presentation.put(CatalogFields.BATCHES, List.of(Map.of(CatalogFields.ID, "LOT" + id.substring(3),
                CatalogFields.BATCH_STOCK, stock)));
        return presentation;
    }
}
