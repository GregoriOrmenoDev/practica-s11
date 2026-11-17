package pe.edu.vallegrande.farmagro.service.impl;

import static pe.edu.vallegrande.farmagro.util.PartialUpdate.setIfPresent;

import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.util.PaginationValidator;

import pe.edu.vallegrande.farmagro.dto.SupplierDTO;
import pe.edu.vallegrande.farmagro.model.Supplier;
import pe.edu.vallegrande.farmagro.repository.SupplierRepository;
import pe.edu.vallegrande.farmagro.repository.sql.GeographicLocationRepository;
import pe.edu.vallegrande.farmagro.service.SupplierService;
import pe.edu.vallegrande.farmagro.util.TextNormalizer;
import pe.edu.vallegrande.farmagro.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final GeographicLocationRepository geographicLocationRepository;
    private final SupplierMapper supplierMapper;
    private final SupplierIdGenerator supplierIdGenerator;


    @Override
    public Flux<SupplierDTO> findAll(int page, int size) {
        return page(supplierRepository.findAll(), page, size);
    }

    @Override
    public Flux<SupplierDTO> findByStatus(Boolean status, int page, int size) {
        return page(supplierRepository.findByStatus(status), page, size);
    }

    // In-memory paging is acceptable: the supplier collection is small.
    private Flux<SupplierDTO> page(Flux<Supplier> suppliers, int page, int size) {
        int offset = PaginationValidator.offset(page, size);
        return suppliers.skip(offset).take(size).map(supplierMapper::toDTO);
    }

    @Override
    public Mono<SupplierDTO> findById(String id) {
        return supplierRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proveedor no encontrado con id: " + id)))
                .map(supplierMapper::toDTO);
    }


    @Override
    public Mono<SupplierDTO> create(SupplierDTO dto) {
        normalizeSupplier(dto);
        return validateGeographicLocation(dto.getGeographicLocationId())
                .then(validateUniqueEmail(dto.getEmail(), null))
                .then(validateUniqueDocument(dto.getDocumentType(), dto.getDocumentNumber(), null))
                .then(supplierIdGenerator.insertNew(dto.getId(), id -> toEntity(dto, id)))
                .map(supplierMapper::toDTO);
    }

    @Override
    public Mono<SupplierDTO> update(String id, SupplierDTO dto) {
        normalizeSupplier(dto);
        return supplierRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proveedor no encontrado con id: " + id)))
                .flatMap(entity -> validateSupplierUpdate(dto, id, entity))
                .flatMap(entity -> updateSupplierFields(entity, dto))
                .map(supplierMapper::toDTO);
    }

    private void normalizeSupplier(SupplierDTO dto) {
        normalizeIdentity(dto);
        normalizeContact(dto);
    }

    private void normalizeIdentity(SupplierDTO dto) {
        dto.setDocumentType(TextNormalizer.code(dto.getDocumentType()));
        dto.setDocumentNumber(TextNormalizer.trim(dto.getDocumentNumber()));
        dto.setBusinessName(TextNormalizer.trim(dto.getBusinessName()));
    }

    private void normalizeContact(SupplierDTO dto) {
        dto.setEmail(TextNormalizer.email(dto.getEmail()));
        dto.setContactName(TextNormalizer.trim(dto.getContactName()));
        dto.setPhone(TextNormalizer.trim(dto.getPhone()));
        dto.setAddress(TextNormalizer.trim(dto.getAddress()));
    }

    private Mono<Supplier> validateSupplierUpdate(SupplierDTO dto, String id, Supplier entity) {
        return validateGeographicLocation(dto.getGeographicLocationId())
                .then(validateUniqueEmail(dto.getEmail(), id))
                .then(validateUniqueDocument(dto.getDocumentType(), dto.getDocumentNumber(), id))
                .thenReturn(entity);
    }

    private Mono<Void> validateGeographicLocation(Integer geographicLocationId) {
        if (geographicLocationId == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El ID geográfico del proveedor es obligatorio"));
        }
        return geographicLocationRepository.existsById(geographicLocationId)
                .flatMap(exists -> exists ? Mono.empty()
                        : Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No existe el ID geográfico " + geographicLocationId + " en el catálogo UBIGEO")));
    }

    private Mono<Supplier> updateSupplierFields(Supplier entity, SupplierDTO dto) {
        setIfPresent(dto.getDocumentType(), entity::setDocumentType);
        setIfPresent(dto.getDocumentNumber(), entity::setDocumentNumber);
        setIfPresent(dto.getBusinessName(), entity::setBusinessName);
        setIfPresent(dto.getContactName(), entity::setContactName);
        setIfPresent(dto.getPhone(), entity::setPhone);
        setIfPresent(dto.getEmail(), entity::setEmail);
        setIfPresent(dto.getAddress(), entity::setAddress);
        setIfPresent(dto.getGeographicLocationId(), entity::setGeographicLocationId);
        setIfPresent(dto.getProductCategories(), entity::setProductCategories);
        setIfPresent(dto.getCertifications(), entity::setCertifications);
        entity.setUpdatedAt(LocalDateTime.now());
        return supplierRepository.save(entity);
    }


    @Override
    public Mono<SupplierDTO> delete(String id) {
        return ValidationUtils.validateEntityExists(
                supplierRepository.findById(id), "proveedor", id
        )
                .flatMap(entity -> ValidationUtils.validateCanDelete(entity.getStatus(), "proveedor")
                        .thenReturn(entity))
                .flatMap(entity -> {
                    entity.setStatus(false);
                    entity.setDeletedAt(LocalDateTime.now());
                    entity.setRestoredAt(null);
                    return supplierRepository.save(entity);
                })
                .map(supplierMapper::toDTO);
    }


    @Override
    public Mono<SupplierDTO> restore(String id) {
        return ValidationUtils.validateEntityExists(
                supplierRepository.findById(id), "proveedor", id
        )
                .flatMap(entity -> ValidationUtils.validateCanRestore(entity.getStatus(), "proveedor")
                        .thenReturn(entity))
                .flatMap(entity -> {
                    entity.setStatus(true);
                    entity.setRestoredAt(LocalDateTime.now());
                    entity.setDeletedAt(null);
                    return supplierRepository.save(entity);
                })
                .map(supplierMapper::toDTO);
    }


    private Mono<Void> validateUniqueEmail(String email, String excludeId) {
        Mono<Boolean> recordExists = (excludeId == null)
                ? supplierRepository.existsByEmailIgnoreCase(email)
                : supplierRepository.existsByEmailIgnoreCaseAndIdNot(email, excludeId);

        return recordExists.flatMap(exists -> exists
                ? Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Ya existe un proveedor con el correo " + email))
                : Mono.empty());
    }

    private Mono<Void> validateUniqueDocument(String documentType, String documentNumber, String excludeId) {
        Mono<Boolean> recordExists = (excludeId == null)
                ? supplierRepository.existsByDocumentTypeAndDocumentNumber(documentType, documentNumber)
                : supplierRepository.existsByDocumentTypeAndDocumentNumberAndIdNot(documentType, documentNumber, excludeId);

        return recordExists.flatMap(exists -> exists
                ? Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Ya existe un proveedor con el documento " + documentType + " " + documentNumber))
                : Mono.empty());
    }


    private Supplier toEntity(SupplierDTO dto, String id) {
        return Supplier.builder()
                .id(id)
                .documentType(dto.getDocumentType())
                .documentNumber(dto.getDocumentNumber())
                .businessName(dto.getBusinessName())
                .contactName(dto.getContactName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .geographicLocationId(dto.getGeographicLocationId())
                .productCategories(dto.getProductCategories())
                .certifications(dto.getCertifications())
                .status(true)
                .createdAt(LocalDateTime.now())
                .build();
    }


}
