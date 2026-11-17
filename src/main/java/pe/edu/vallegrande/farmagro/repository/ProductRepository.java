package pe.edu.vallegrande.farmagro.repository;

import pe.edu.vallegrande.farmagro.model.Product;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductRepository extends ReactiveMongoRepository<Product, String> {

    Flux<Product> findByStatus(Boolean status);

    Flux<Product> findByCategories(String category);

    Mono<Boolean> existsByNameIgnoreCase(String name);

    Mono<Boolean> existsBySanitaryRegistrationIgnoreCase(String sanitaryRegistration);

    Mono<Boolean> existsByNameIgnoreCaseAndIdNot(String name, String id);

    Mono<Boolean> existsBySanitaryRegistrationIgnoreCaseAndIdNot(String sanitaryRegistration, String id);
}
