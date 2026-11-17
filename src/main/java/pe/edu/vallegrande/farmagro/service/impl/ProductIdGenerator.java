package pe.edu.vallegrande.farmagro.service.impl;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.farmagro.model.Product;
import pe.edu.vallegrande.farmagro.repository.ProductRepository;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Component
final class ProductIdGenerator {

    private static final String ENTITY_NAME = "producto";
    private static final BusinessIdSequence.IdFormat ID_FORMAT =
            new BusinessIdSequence.IdFormat(AppConstants.PRODUCT_ID_PREFIX, ENTITY_NAME, AppConstants.PRODUCT_ID_PATTERN);

    private final BusinessIdSequence<Product> sequence;

    ProductIdGenerator(ProductRepository productRepository) {
        this.sequence = new BusinessIdSequence<>(ID_FORMAT, productRepository, Product::getId);
    }

    Mono<Product> insertNew(String providedId, Function<String, Product> entityFactory) {
        return sequence.insertNew(providedId, entityFactory);
    }
}
