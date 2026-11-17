package pe.edu.vallegrande.farmagro.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.vallegrande.farmagro.dto.CustomerDTO;
import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import pe.edu.vallegrande.farmagro.model.CustomerEmbedded;
import pe.edu.vallegrande.farmagro.model.Person;
import pe.edu.vallegrande.farmagro.model.PersonRole;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class PartialUpdateTest {

    @Test
    @DisplayName("Presentation merge keeps stored fields the client did not send and never takes stock from the client")
    void presentationMergeShouldKeepOmittedFields() {
        Map<String, Object> stored = new HashMap<>(Map.of(
                "_id", "PST001", "label", "Jug 1L", "sale_price", 38.0, "current_stock", 110,
                "shelf_life_days", 730, "temp_max_c", 35.0, "batches", List.of(Map.of("_id", "LOT001"))));
        Map<String, Object> incoming = new HashMap<>(Map.of(
                "_id", "PST001", "label", "Jug 1 L", "sale_price", 40.0, "current_stock", 999));

        @SuppressWarnings("unchecked")
        Map<String, Object> merged = (Map<String, Object>) new ProductInventoryMerger()
                .merge(List.of(stored), List.of(incoming)).get(0);

        assertEquals("Jug 1 L", merged.get("label"));
        assertEquals(40.0, merged.get("sale_price"));
        assertEquals(110, merged.get("current_stock"));
        assertEquals(730, merged.get("shelf_life_days"));
        assertEquals(35.0, merged.get("temp_max_c"));
        assertEquals(stored.get("batches"), merged.get("batches"));
    }

    @Test
    @DisplayName("Person update ignores null fields and keeps the customer status and creation date")
    void personUpdateShouldKeepOmittedFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        Person stored = Person.builder()
                .id("PER006").documentType("DNI").documentNumber("12345678").firstNames("Ana").lastNames("Diaz")
                .phone("999888777").email("ana@mail.com").role(PersonRole.CUSTOMER.toStorageValue())
                .avatarUrl("https://img/ana.png")
                .customer(CustomerEmbedded.builder().address("Calle 1").geographicLocationId(1)
                        .status(false).createdAt(createdAt).build())
                .build();
        PersonDTO changes = PersonDTO.builder()
                .documentType("DNI").documentNumber("12345678").firstNames("Ana María").email("ana@mail.com")
                .role(PersonRole.CUSTOMER)
                .customer(CustomerDTO.builder().address("Calle 2").geographicLocationId(1).build())
                .build();

        Person updated = new PersonEntityFactory(new PasswordHasher()).applyUpdate(stored, changes);

        assertEquals("Ana María", updated.getFirstNames());
        assertEquals("Diaz", updated.getLastNames());
        assertEquals("999888777", updated.getPhone());
        assertEquals("https://img/ana.png", updated.getAvatarUrl());
        assertEquals("Calle 2", updated.getCustomer().getAddress());
        assertFalse(updated.getCustomer().getStatus());
        assertEquals(createdAt, updated.getCustomer().getCreatedAt());
    }

    @Test
    @DisplayName("Changing a customer into staff drops the customer subdocument")
    void personUpdateShouldDropCustomerForStaff() {
        Person stored = Person.builder().id("PER006").role(PersonRole.CUSTOMER.toStorageValue())
                .customer(CustomerEmbedded.builder().address("Calle 1").build()).build();
        PersonDTO changes = PersonDTO.builder().role(PersonRole.VENDOR).build();

        Person updated = new PersonEntityFactory(new PasswordHasher()).applyUpdate(stored, changes);

        assertEquals(PersonRole.VENDOR.toStorageValue(), updated.getRole());
        assertNull(updated.getCustomer());
    }
}
