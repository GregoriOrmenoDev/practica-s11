package pe.edu.vallegrande.farmagro.repository;

import pe.edu.vallegrande.farmagro.model.Supplier;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SupplierRepository extends ReactiveMongoRepository<Supplier, String> {

    Flux<Supplier> findByStatus(Boolean status);

    Mono<Boolean> existsByEmailIgnoreCase(String email);

    Mono<Boolean> existsByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    Mono<Boolean> existsByEmailIgnoreCaseAndIdNot(String email, String id);

    Mono<Boolean> existsByDocumentTypeAndDocumentNumberAndIdNot(String documentType, String documentNumber, String id);
}
