package pe.edu.vallegrande.farmagro.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.vallegrande.farmagro.dto.SaleDTO;
import pe.edu.vallegrande.farmagro.model.Person;
import pe.edu.vallegrande.farmagro.model.sql.Purchase;
import pe.edu.vallegrande.farmagro.model.sql.Sale;
import pe.edu.vallegrande.farmagro.repository.PersonRepository;
import pe.edu.vallegrande.farmagro.repository.SupplierRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartyRevalidationTest {

    private final PersonRepository personRepository = mock(PersonRepository.class);
    private final SupplierRepository supplierRepository = mock(SupplierRepository.class);

    @Test
    @DisplayName("Purchase update with the original supplier and user skips their checks even if deactivated")
    void purchaseUpdateShouldNotRecheckUnchangedParties() {
        Purchase stored = Purchase.builder().mongoIdSupplier("SUP001").mongoIdUser("PER004").build();

        StepVerifier.create(new PurchasePartyValidator(supplierRepository, personRepository)
                        .validateChanged(stored, "SUP001", "PER004"))
                .verifyComplete();

        verify(supplierRepository, never()).findById("SUP001");
        verify(personRepository, never()).findById("PER004");
    }

    @Test
    @DisplayName("Purchase update that changes the user validates the new one")
    void purchaseUpdateShouldValidateChangedUser() {
        Purchase stored = Purchase.builder().mongoIdSupplier("SUP001").mongoIdUser("PER004").build();
        when(personRepository.findById("PER005")).thenReturn(Mono.just(person("PER005", "DELIV", true, "DNI")));

        StepVerifier.create(new PurchasePartyValidator(supplierRepository, personRepository)
                        .validateChanged(stored, "SUP001", "PER005"))
                .expectErrorSatisfies(error -> assertBadRequest(error, "ADMIN o ALMAC"))
                .verify();
    }

    @Test
    @DisplayName("Sale update keeps a deactivated original customer and seller, and returns the customer")
    void saleUpdateShouldNotRecheckUnchangedParties() {
        Sale stored = Sale.builder().mongoIdCustomer("PER006").mongoIdUser("PER002").build();
        when(personRepository.findById("PER006")).thenReturn(Mono.just(person("PER006", "CLIENT", false, "DNI")));

        StepVerifier.create(new SalePartyValidator(personRepository).validateChangedParties(stored, saleDto("PER006", "PER002")))
                .assertNext(customer -> assertEquals("PER006", customer.getId()))
                .verifyComplete();

        verify(personRepository, never()).findById("PER002");
    }

    @Test
    @DisplayName("Sale update that switches to an inactive customer is rejected")
    void saleUpdateShouldValidateChangedCustomer() {
        Sale stored = Sale.builder().mongoIdCustomer("PER006").mongoIdUser("PER002").build();
        when(personRepository.findById("PER007")).thenReturn(Mono.just(person("PER007", "CLIENT", false, "DNI")));

        StepVerifier.create(new SalePartyValidator(personRepository).validateChangedParties(stored, saleDto("PER007", "PER002")))
                .expectErrorSatisfies(error -> assertBadRequest(error, "inactivo"))
                .verify();
    }

    private void assertBadRequest(Throwable error, String expectedText) {
        assertTrue(error instanceof ResponseStatusException);
        ResponseStatusException exception = (ResponseStatusException) error;
        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains(expectedText), exception.getReason());
    }

    private SaleDTO saleDto(String customerId, String sellerId) {
        SaleDTO dto = new SaleDTO();
        dto.setMongoIdCustomer(customerId);
        dto.setMongoIdUser(sellerId);
        return dto;
    }

    private Person person(String id, String role, boolean active, String documentType) {
        return Person.builder().id(id).role(role).status(active).documentType(documentType)
                .firstNames("Test").lastNames("Person").build();
    }
}
