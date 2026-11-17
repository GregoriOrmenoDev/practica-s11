package pe.edu.vallegrande.farmagro.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import pe.edu.vallegrande.farmagro.model.Person;
import pe.edu.vallegrande.farmagro.repository.PersonRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonCollaboratorsTest {

    @Test
    @DisplayName("Person ID generator inserts with the next sequence after the highest stored one")
    void personIdGeneratorShouldUseNextSequence() {
        PersonRepository repository = mock(PersonRepository.class);
        when(repository.findAll()).thenReturn(Flux.just(person("PER001"), person("PER009"), person("LEGACY")));
        when(repository.existsById(anyString())).thenReturn(Mono.just(false));
        when(repository.insert(any(Person.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(new PersonIdGenerator(repository).insertNew(null, this::person))
                .assertNext(saved -> assertEquals("PER010", saved.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Person ID generator retries with a fresh ID when a concurrent insert took the same _id")
    void personIdGeneratorShouldRetryOnIdCollision() {
        PersonRepository repository = mock(PersonRepository.class);
        when(repository.findAll()).thenReturn(Flux.just(person("PER009")), Flux.just(person("PER009"), person("PER010")));
        when(repository.existsById(anyString())).thenReturn(Mono.just(false));
        when(repository.insert(any(Person.class)))
                .thenReturn(Mono.error(new DuplicateKeyException("E11000 duplicate key error index: _id_ dup key")))
                .thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(new PersonIdGenerator(repository).insertNew(null, this::person))
                .assertNext(saved -> assertTrue(Integer.parseInt(saved.getId().substring(3)) > 10,
                        "retry must use an ID after the collided PER010, got " + saved.getId()))
                .verifyComplete();
        verify(repository, times(2)).insert(any(Person.class));
    }

    @Test
    @DisplayName("Person ID generator does not retry when another unique index (e.g. email) collides")
    void personIdGeneratorShouldNotRetryOnOtherDuplicates() {
        PersonRepository repository = mock(PersonRepository.class);
        when(repository.findAll()).thenReturn(Flux.just(person("PER001")));
        when(repository.existsById(anyString())).thenReturn(Mono.just(false));
        when(repository.insert(any(Person.class)))
                .thenReturn(Mono.error(new DuplicateKeyException("E11000 duplicate key error index: email_1 dup key")));

        StepVerifier.create(new PersonIdGenerator(repository).insertNew(null, this::person))
                .expectError(DuplicateKeyException.class)
                .verify();
        verify(repository, times(1)).insert(any(Person.class));
    }

    @Test
    @DisplayName("Person ID generator rejects a provided ID with the wrong format")
    void personIdGeneratorShouldRejectInvalidId() {
        PersonRepository repository = mock(PersonRepository.class);

        StepVerifier.create(new PersonIdGenerator(repository).insertNew("PERX1", this::person))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    @DisplayName("Password hasher keeps existing hashes and verifies legacy plain text")
    void passwordHasherShouldHandleHashedAndLegacyPasswords() {
        PasswordHasher passwordHasher = new PasswordHasher();
        String hash = passwordHasher.encode("Secret123*");

        assertEquals(hash, passwordHasher.hashIfPlain(hash));
        assertTrue(passwordHasher.matches("Secret123*", hash));
        assertTrue(passwordHasher.matches("legacy", "legacy"));
        assertFalse(passwordHasher.matches("wrong", hash));
    }

    @Test
    @DisplayName("Person normalizer trims fields and fills the RUC last-name placeholder")
    void personNormalizerShouldNormalizeFields() {
        PersonDTO person = PersonDTO.builder()
                .email("  Ana@Mail.COM ")
                .documentType(" ruc ")
                .documentNumber(" 20123456789 ")
                .firstNames(" Agro SAC ")
                .lastNames("  ")
                .build();

        new PersonNormalizer().normalize(person);

        assertEquals("ana@mail.com", person.getEmail());
        assertEquals("RUC", person.getDocumentType());
        assertEquals("20123456789", person.getDocumentNumber());
        assertEquals("Agro SAC", person.getFirstNames());
        assertEquals("-", person.getLastNames());
    }

    private Person person(String id) {
        return Person.builder().id(id).build();
    }
}
