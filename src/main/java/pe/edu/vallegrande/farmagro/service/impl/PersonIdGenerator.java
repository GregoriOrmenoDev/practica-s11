package pe.edu.vallegrande.farmagro.service.impl;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.farmagro.model.Person;
import pe.edu.vallegrande.farmagro.repository.PersonRepository;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Component
final class PersonIdGenerator {

    private static final String ENTITY_NAME = "persona";
    private static final BusinessIdSequence.IdFormat ID_FORMAT =
            new BusinessIdSequence.IdFormat(AppConstants.PERSON_ID_PREFIX, ENTITY_NAME, AppConstants.PERSON_ID_PATTERN);

    private final BusinessIdSequence<Person> sequence;

    PersonIdGenerator(PersonRepository personRepository) {
        this.sequence = new BusinessIdSequence<>(ID_FORMAT, personRepository, Person::getId);
    }

    Mono<Person> insertNew(String providedId, Function<String, Person> entityFactory) {
        return sequence.insertNew(providedId, entityFactory);
    }
}
