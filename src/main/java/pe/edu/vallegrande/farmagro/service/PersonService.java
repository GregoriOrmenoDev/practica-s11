package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PersonService {

    Flux<PersonDTO> findAll();

    Flux<PersonDTO> findAll(int page, int size);

    Flux<PersonDTO> findByStatus(Boolean status, int page, int size);

    Flux<PersonDTO> findByRole(String role, int page, int size);

    Flux<PersonDTO> findByRoleAndStatus(String role, Boolean status, int page, int size);

    Mono<PersonDTO> findById(String id);

    /**
     * Creates a person after enforcing the domain uniqueness rules: email is unique
     * case-insensitively and the document (type + number) is unique. A {@code CUSTOMER}
     * must include the embedded customer sub-document, while any other role must leave
     * it null. The {@code status} field is always initialized as active.
     *
     * @param dto person data to persist
     * @return the stored person
     */
    Mono<PersonDTO> create(PersonDTO dto);

    /**
     * Updates an existing person while revalidating the uniqueness and role/customer
     * constraints applied on creation. The {@code status} field cannot be changed here;
     * use {@link #delete} or {@link #restore} for that.
     *
     * @param id existing person identifier
     * @param dto updated person data
     * @return the stored person; it emits NOT_FOUND for an unknown ID
     */
    Mono<PersonDTO> update(String id, PersonDTO dto);

    /**
     * Marks a person and its embedded customer inactive and sets their deletion timestamps
     * without removing the stored record.
     *
     * @param id existing person identifier
     * @return a Mono with the updated inactive person; it emits NOT_FOUND for an unknown ID
     *         and BAD_REQUEST when the person is already inactive
     */
    Mono<PersonDTO> delete(String id);

    /**
     * Reactivates a soft-deleted person and its embedded customer, clearing their deletion
     * timestamps and setting their restoration timestamps.
     *
     * @param id identifier of a previously deleted person
     * @return a Mono with the updated active person; it emits NOT_FOUND for an unknown ID
     *         and BAD_REQUEST when the person is already active
     */
    Mono<PersonDTO> restore(String id);

}
