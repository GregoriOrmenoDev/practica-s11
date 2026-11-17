package pe.edu.vallegrande.farmagro.service.impl;

import static pe.edu.vallegrande.farmagro.util.PartialUpdate.setIfPresent;

import pe.edu.vallegrande.farmagro.dto.ProductDTO;
import pe.edu.vallegrande.farmagro.model.Product;
import pe.edu.vallegrande.farmagro.repository.ProductRepository;
import pe.edu.vallegrande.farmagro.service.ProductService;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.util.PaginationValidator;
import pe.edu.vallegrande.farmagro.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductIdGenerator productIdGenerator;
    private final ProductPresentationValidator presentationValidator;
    private final ProductInventoryMerger inventoryMerger;
    private final ProductEntityFactory productEntityFactory;

    @Override
    public Flux<ProductDTO> findAll(int page, int size) {
        return page(productRepository.findAll(), page, size);
    }

    @Override
    public Flux<ProductDTO> findAllActive(int page, int size) {
        return page(productRepository.findByStatus(true), page, size);
    }

    @Override
    public Flux<ProductDTO> findAllDeleted(int page, int size) {
        return page(productRepository.findByStatus(false), page, size);
    }

    // In-memory paging is acceptable: the catalog is small and Mongo returns it in insertion order.
    private Flux<ProductDTO> page(Flux<Product> products, int page, int size) {
        int offset = PaginationValidator.offset(page, size);
        return products.skip(offset).take(size).map(productMapper::toDTO);
    }

    @Override
    public Flux<ProductDTO> findByCategory(String category) {
        return productRepository.findByCategories(category).take(AppConstants.DEFAULT_QUERY_LIMIT).map(productMapper::toDTO);
    }

    @Override
    public Mono<ProductDTO> findById(String id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Producto no encontrado con id: " + id)))
                .map(productMapper::toDTO);
    }

    @Override
    public Mono<ProductDTO> create(ProductDTO dto) {
        normalizeProduct(dto);
        dto.setPresentations(productEntityFactory.withBarcodes(dto.getPresentations()));
        return presentationValidator.validate(dto)
                .then(validateUniqueName(dto.getName(), null))
                .then(validateUniqueRegistration(dto.getSanitaryRegistration(), null))
                .then(productIdGenerator.insertNew(dto.getId(), id -> productEntityFactory.toEntity(dto, id)))
                .map(productMapper::toDTO);
    }

    @Override
    public Mono<ProductDTO> update(String id, ProductDTO dto) {
        normalizeProduct(dto);
        return Mono.defer(() -> updateOnce(id, dto)).retryWhen(OptimisticRetry.onVersionConflict());
    }

    private Mono<ProductDTO> updateOnce(String id, ProductDTO dto) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Producto no encontrado con id: " + id)))
                .flatMap(entity -> {
                    mergePresentationsIfProvided(dto, entity);
                    return validateProductUpdate(dto, id, entity);
                })
                .flatMap(entity -> updateProductFields(entity, dto))
                .map(productMapper::toDTO);
    }

    private void normalizeProduct(ProductDTO dto) {
        if (dto.getName() != null) dto.setName(dto.getName().trim());
        if (dto.getBrand() != null) dto.setBrand(dto.getBrand().trim());
        if (dto.getSanitaryRegistration() != null) {
            dto.setSanitaryRegistration(dto.getSanitaryRegistration().trim().toUpperCase(Locale.ROOT));
        }
    }

    private Mono<Product> validateProductUpdate(ProductDTO dto, String id, Product entity) {
        return presentationValidator.validateIfProvided(dto, entity.getPresentations())
                .then(validateUniqueName(dto.getName(), id))
                .then(validateUniqueRegistration(dto.getSanitaryRegistration(), id))
                .thenReturn(entity);
    }

    private void mergePresentationsIfProvided(ProductDTO dto, Product entity) {
        if (dto.getPresentations() != null && !dto.getPresentations().isEmpty()) {
            dto.setPresentations(productEntityFactory.withBarcodes(
                    inventoryMerger.merge(entity.getPresentations(), dto.getPresentations())));
        }
    }

    private Mono<Product> updateProductFields(Product entity, ProductDTO dto) {
        setIfPresent(dto.getName(), entity::setName);
        setIfPresent(dto.getBrand(), entity::setBrand);
        if (dto.getSanitaryRegistration() != null) {
            // Blank clears the registration; stored as null so the partial unique index ignores it.
            entity.setSanitaryRegistration(dto.getSanitaryRegistration().isBlank() ? null : dto.getSanitaryRegistration());
        }
        setIfPresent(dto.getDescription(), entity::setDescription);
        setIfPresent(dto.getActiveIngredient(), entity::setActiveIngredient);
        setIfPresent(dto.getToxicityLevel(), entity::setToxicityLevel);
        setIfPresent(dto.getRequiresLicense(), entity::setRequiresLicense);
        setIfPresent(dto.getCategories(), entity::setCategories);
        setIfPresent(dto.getImageUrl(), entity::setImageUrl);
        setIfPresent(dto.getTechnicalSpecifications(), entity::setTechnicalSpecifications);

        updatePresentationsIfProvided(entity, dto.getPresentations());

        entity.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(entity);
    }

    private void updatePresentationsIfProvided(Product entity, List<Object> presentations) {
        if (presentations != null && !presentations.isEmpty()) {
            entity.setPresentations(inventoryMerger.merge(entity.getPresentations(), presentations));
        }
        // Omitted presentations must not wipe stored inventory.
    }

    @Override
    public Mono<Void> delete(String id) {
        return Mono.defer(() -> ValidationUtils.validateEntityExists(productRepository.findById(id), "producto", id)
                        .flatMap(entity -> ValidationUtils.validateCanDelete(entity.getStatus(), "producto")
                                .thenReturn(entity))
                        .flatMap(entity -> {
                            entity.setStatus(false);
                            entity.setDeletedAt(LocalDateTime.now());
                            entity.setRestoredAt(null);
                            return productRepository.save(entity);
                        }))
                .retryWhen(OptimisticRetry.onVersionConflict())
                .then();
    }

    @Override
    public Mono<ProductDTO> restore(String id) {
        return Mono.defer(() -> ValidationUtils.validateEntityExists(productRepository.findById(id), "producto", id)
                        .flatMap(entity -> ValidationUtils.validateCanRestore(entity.getStatus(), "producto")
                                .thenReturn(entity))
                        .flatMap(entity -> {
                            entity.setStatus(true);
                            entity.setRestoredAt(LocalDateTime.now());
                            entity.setDeletedAt(null);
                            return productRepository.save(entity);
                        }))
                .retryWhen(OptimisticRetry.onVersionConflict())
                .map(productMapper::toDTO);
    }

    private Mono<Void> validateUniqueName(String name, String excludeId) {
        Mono<Boolean> recordExists = (excludeId == null)
                ? productRepository.existsByNameIgnoreCase(name)
                : productRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId);
        return recordExists.flatMap(exists -> exists
                ? Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Ya existe un producto con el nombre " + name))
                : Mono.empty());
    }

    private Mono<Void> validateUniqueRegistration(String registration, String excludeId) {
        if (registration == null || registration.isBlank()) return Mono.empty();
        Mono<Boolean> recordExists = (excludeId == null)
                ? productRepository.existsBySanitaryRegistrationIgnoreCase(registration)
                : productRepository.existsBySanitaryRegistrationIgnoreCaseAndIdNot(registration, excludeId);
        return recordExists.flatMap(exists -> exists
                ? Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Ya existe un producto con el registro sanitario " + registration))
                : Mono.empty());
    }
}
