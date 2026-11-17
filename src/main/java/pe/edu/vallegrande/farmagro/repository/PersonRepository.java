package pe.edu.vallegrande.farmagro.repository;

import pe.edu.vallegrande.farmagro.model.Person;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PersonRepository extends ReactiveMongoRepository<Person, String> {

    Flux<Person> findByStatus(Boolean status);

    Flux<Person> findByRole(String role);

    Flux<Person> findByRoleAndStatus(String role, Boolean status);

    Mono<Person> findByEmailIgnoreCase(String email);

    Mono<Boolean> existsByEmailIgnoreCase(String email);

    Mono<Boolean> existsByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    Mono<Boolean> existsByEmailIgnoreCaseAndIdNot(String email, String id);

    Mono<Boolean> existsByDocumentTypeAndDocumentNumberAndIdNot(String documentType, String documentNumber, String id);
}
