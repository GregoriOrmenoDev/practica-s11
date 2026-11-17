package pe.edu.vallegrande.farmagro.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.vallegrande.farmagro.dto.CustomerDTO;
import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import pe.edu.vallegrande.farmagro.model.PersonRole;
import pe.edu.vallegrande.farmagro.repository.PersonRepository;
import pe.edu.vallegrande.farmagro.repository.sql.GeographicLocationRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersonLocationTest {

    private static final String HEADER =
            "id;documentType;documentNumber;firstNames;lastNames;phone;email;role;address;geographicLocationId;initialPassword\n";

    @Test
    @DisplayName("An unknown UBIGEO is rejected instead of being replaced by the default district")
    void validatorShouldRejectUnknownLocation() {
        PersonRepository personRepository = mock(PersonRepository.class);
        GeographicLocationRepository locationRepository = mock(GeographicLocationRepository.class);
        when(personRepository.existsByEmailIgnoreCase(anyString())).thenReturn(Mono.just(false));
        when(personRepository.existsByDocumentTypeAndDocumentNumber(anyString(), anyString())).thenReturn(Mono.just(false));
        when(locationRepository.existsById(99)).thenReturn(Mono.just(false));
        PersonDTO customer = PersonDTO.builder()
                .documentType("DNI").documentNumber("12345678").firstNames("Ana").lastNames("Diaz")
                .email("ana@mail.com").role(PersonRole.CUSTOMER)
                .customer(CustomerDTO.builder().address("Calle 1").geographicLocationId(99).build())
                .build();

        StepVerifier.create(new PersonValidator(personRepository, locationRepository).validate(customer, null))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof ResponseStatusException);
                    assertEquals(400, ((ResponseStatusException) error).getStatusCode().value());
                    assertTrue(((ResponseStatusException) error).getReason().contains("ubigeo 99"));
                })
                .verify();
        assertEquals(99, customer.getCustomer().getGeographicLocationId());
    }

    @Test
    @DisplayName("Imported customers without UBIGEO stay without location; a non-numeric one is reported")
    void importShouldNotInventLocations() {
        String csv = HEADER
                + ";DNI;72345678;Juan;Perez;987654321;juan@mail.com;CUSTOMER;Av. Uno 1;;\n"
                + ";DNI;72345679;Rosa;Diaz;987654322;rosa@mail.com;CUSTOMER;Av. Dos 2;abc;\n";

        StepVerifier.create(new PersonImportFileParserImpl()
                        .parseCsv(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))).collectList())
                .assertNext(persons -> {
                    assertNull(persons.get(0).getCustomer().getGeographicLocationId());
                    assertEquals(0, persons.get(1).getCustomer().getGeographicLocationId());
                })
                .verifyComplete();
    }
}
