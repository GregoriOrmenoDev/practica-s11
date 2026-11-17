package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.util.PaginationValidator;

import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import pe.edu.vallegrande.farmagro.model.Person;
import pe.edu.vallegrande.farmagro.repository.PersonRepository;
import pe.edu.vallegrande.farmagro.service.PersonService;
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
public class PersonServiceImpl implements PersonService {

    private static final String PERSON_ENTITY = "persona";
    private static final String PERSON_NOT_FOUND_MESSAGE = "Person no encontrada con id: ";

    private final PersonRepository personRepository;
    private final PersonSalesRoleValidator personSalesRoleValidator;
    private final PersonMapper personMapper;
    private final PersonNormalizer personNormalizer;
    private final PersonValidator personValidator;
    private final PersonIdGenerator personIdGenerator;
    private final PersonEntityFactory personEntityFactory;

    @Override
    public Flux<PersonDTO> findAll() {
        return personRepository.findAll()
                .take(AppConstants.DEFAULT_QUERY_LIMIT)
                .map(personMapper::toDTO);
    }

    @Override
    public Flux<PersonDTO> findAll(int page, int size) {
        int offset = PaginationValidator.offset(page, size);
        return personRepository.findAll()
                // In-memory paging is acceptable: the person collection is small.
                .skip(offset)
                .take(size)
                .map(personMapper::toDTO);
    }

    @Override
    public Flux<PersonDTO> findByStatus(Boolean status, int page, int size) {
        int offset = PaginationValidator.offset(page, size);
        return personRepository.findByStatus(status)
                .skip(offset)
                .take(size)
                .map(personMapper::toDTO);
    }

    @Override
    public Flux<PersonDTO> findByRole(String role, int page, int size) {
        int offset = PaginationValidator.offset(page, size);
        return personRepository.findByRole(role)
                .skip(offset)
                .take(size)
                .map(personMapper::toDTO);
    }

    @Override
    public Flux<PersonDTO> findByRoleAndStatus(String role, Boolean status, int page, int size) {
        int offset = PaginationValidator.offset(page, size);
        return personRepository.findByRoleAndStatus(role, status)
                .skip(offset)
                .take(size)
                .map(personMapper::toDTO);
    }

    @Override
    public Mono<PersonDTO> findById(String id) {
        return personRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, PERSON_NOT_FOUND_MESSAGE + id)))
                .map(personMapper::toDTO);
    }


    @Override
    public Mono<PersonDTO> create(PersonDTO dto) {
        personNormalizer.normalize(dto);
        return personValidator.validate(dto, null)
                .then(personIdGenerator.insertNew(dto.getId(), personId -> personEntityFactory.toEntity(dto, personId)))
                .map(personMapper::toDTO);
    }

    @Override
    public Mono<PersonDTO> update(String id, PersonDTO dto) {
        personNormalizer.normalize(dto);
        return personRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, PERSON_NOT_FOUND_MESSAGE + id)))
                .flatMap(entity -> personValidator.validate(dto, id)
                        .then(personSalesRoleValidator.validateRoleChange(entity, dto.getRole()))
                        .thenReturn(entity))
                .map(entity -> personEntityFactory.applyUpdate(entity, dto))
                .flatMap(personRepository::save)
                .map(personMapper::toDTO);
    }

    @Override
    public Mono<PersonDTO> delete(String id) {
        return ValidationUtils.validateEntityExists(personRepository.findById(id), PERSON_ENTITY, id)
                .flatMap(entity -> ValidationUtils.validateCanDelete(entity.getStatus(), PERSON_ENTITY)
                        .thenReturn(entity))
                .flatMap(entity -> {
                    markDeleted(entity, LocalDateTime.now());
                    return personRepository.save(entity);
                })
                .map(personMapper::toDTO);
    }

    @Override
    public Mono<PersonDTO> restore(String id) {
        return ValidationUtils.validateEntityExists(personRepository.findById(id), PERSON_ENTITY, id)
                .flatMap(entity -> ValidationUtils.validateCanRestore(entity.getStatus(), PERSON_ENTITY)
                        .thenReturn(entity))
                .flatMap(entity -> {
                    markRestored(entity, LocalDateTime.now());
                    return personRepository.save(entity);
                })
                .map(personMapper::toDTO);
    }

    private void markDeleted(Person entity, LocalDateTime deletedAt) {
        entity.setStatus(false);
        entity.setDeletedAt(deletedAt);
        entity.setRestoredAt(null);
        if (entity.getCustomer() != null) {
            entity.getCustomer().setStatus(false);
            entity.getCustomer().setDeletedAt(deletedAt);
            entity.getCustomer().setRestoredAt(null);
        }
    }

    private void markRestored(Person entity, LocalDateTime restoredAt) {
        entity.setStatus(true);
        entity.setRestoredAt(restoredAt);
        entity.setDeletedAt(null);
        if (entity.getCustomer() != null) {
            entity.getCustomer().setStatus(true);
            entity.getCustomer().setRestoredAt(restoredAt);
            entity.getCustomer().setDeletedAt(null);
        }
    }
}
