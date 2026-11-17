package pe.edu.vallegrande.farmagro.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.vallegrande.farmagro.repository.sql.SaleRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvoiceNumberAssignerTest {

    @Test
    @DisplayName("Assigns the next correlative of the series that matches the voucher type")
    void shouldAssignNextCorrelativeBySeries() {
        SaleRepository repository = mock(SaleRepository.class);
        when(repository.findMaxCorrelative("F001-%")).thenReturn(Mono.just(528));
        when(repository.findMaxCorrelative("B001-%")).thenReturn(Mono.just(0));
        InvoiceNumberAssigner assigner = new InvoiceNumberAssigner(repository);

        StepVerifier.create(assigner.nextNumber("INVOICE")).expectNext("F001-0529").verifyComplete();
        StepVerifier.create(assigner.nextNumber("RECEIPT")).expectNext("B001-0001").verifyComplete();
    }

    @Test
    @DisplayName("Rejects a client number whose series does not match the voucher type")
    void shouldValidateSeries() {
        InvoiceNumberAssigner assigner = new InvoiceNumberAssigner(mock(SaleRepository.class));

        StepVerifier.create(assigner.validateSeries("F001-0010", "INVOICE")).verifyComplete();
        StepVerifier.create(assigner.validateSeries("B001-0010", "INVOICE"))
                .expectError(ResponseStatusException.class).verify();
        StepVerifier.create(assigner.validateSeries("F001-0010", "RECEIPT"))
                .expectError(ResponseStatusException.class).verify();
        assertTrue(assigner.needsNumber("  "));
        assertFalse(assigner.needsNumber("B001-0001"));
    }
}
