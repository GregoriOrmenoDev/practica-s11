package pe.edu.vallegrande.farmagro.service.impl;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.farmagro.model.Supplier;
import pe.edu.vallegrande.farmagro.repository.SupplierRepository;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Component
final class SupplierIdGenerator {

    private static final String ENTITY_NAME = "proveedor";
    private static final BusinessIdSequence.IdFormat ID_FORMAT =
            new BusinessIdSequence.IdFormat(AppConstants.SUPPLIER_ID_PREFIX, ENTITY_NAME, AppConstants.SUPPLIER_ID_PATTERN);

    private final BusinessIdSequence<Supplier> sequence;

    SupplierIdGenerator(SupplierRepository supplierRepository) {
        this.sequence = new BusinessIdSequence<>(ID_FORMAT, supplierRepository, Supplier::getId);
    }

    Mono<Supplier> insertNew(String providedId, Function<String, Supplier> entityFactory) {
        return sequence.insertNew(providedId, entityFactory);
    }
}
