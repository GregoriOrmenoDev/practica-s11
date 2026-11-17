package pe.edu.vallegrande.farmagro.service.impl;

import static pe.edu.vallegrande.farmagro.util.PartialUpdate.setIfPresent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.farmagro.dto.CustomerDTO;
import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import pe.edu.vallegrande.farmagro.dto.RegisterRequestDTO;
import pe.edu.vallegrande.farmagro.model.CustomerEmbedded;
import pe.edu.vallegrande.farmagro.model.Person;
import pe.edu.vallegrande.farmagro.model.PersonRole;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.util.TextNormalizer;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
@RequiredArgsConstructor
final class PersonEntityFactory {

    // Same initial password that the import template assigns.
    private static final String DEFAULT_PERSON_PASSWORD = "Farmagro2026*";
    private static final String DEFAULT_CUSTOMER_ADDRESS = "Dirección no especificada";

    private final PasswordHasher passwordHasher;

    Person toEntity(PersonDTO person, String personId) {
        return Person.builder()
                .id(personId)
                .documentType(person.getDocumentType())
                .documentNumber(person.getDocumentNumber())
                .firstNames(person.getFirstNames())
                .lastNames(person.getLastNames())
                .phone(person.getPhone())
                .email(person.getEmail())
                .passwordHash(passwordHasher.hashIfPlain(initialPassword(person.getPasswordHash())))
                .role(storageRole(person.getRole()))
                .avatarUrl(person.getAvatarUrl())
                .status(true)
                .createdAt(LocalDateTime.now())
                .customer(toCustomerEmbedded(person.getCustomer()))
                .build();
    }

    Person applyUpdate(Person entity, PersonDTO changes) {
        setIfPresent(changes.getDocumentType(), entity::setDocumentType);
        setIfPresent(changes.getDocumentNumber(), entity::setDocumentNumber);
        setIfPresent(changes.getFirstNames(), entity::setFirstNames);
        setIfPresent(changes.getLastNames(), entity::setLastNames);
        setIfPresent(changes.getPhone(), entity::setPhone);
        setIfPresent(changes.getEmail(), entity::setEmail);
        if (!TextNormalizer.isBlank(changes.getPasswordHash())) {
            entity.setPasswordHash(passwordHasher.hashIfPlain(changes.getPasswordHash()));
        }
        setIfPresent(storageRole(changes.getRole()), entity::setRole);
        setIfPresent(changes.getAvatarUrl(), entity::setAvatarUrl);
        entity.setCustomer(PersonRole.CUSTOMER.toStorageValue().equals(entity.getRole())
                ? mergeCustomer(entity.getCustomer(), changes.getCustomer())
                : null);
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    // Keeps the stored customer status and audit dates unless the request sends new values.
    private CustomerEmbedded mergeCustomer(CustomerEmbedded stored, CustomerDTO changes) {
        if (stored == null || changes == null) {
            return changes == null ? stored : toCustomerEmbedded(changes);
        }
        setIfPresent(changes.getAddress(), stored::setAddress);
        setIfPresent(changes.getGeographicLocationId(), stored::setGeographicLocationId);
        setIfPresent(changes.getStatus(), stored::setStatus);
        stored.setUpdatedAt(LocalDateTime.now());
        return stored;
    }

    Person fromRegistration(RegisterRequestDTO registration, String email, String personId) {
        LocalDateTime registeredAt = LocalDateTime.now();
        return Person.builder()
                .id(personId)
                .documentType(registration.getDocumentType())
                .documentNumber(registration.getDocumentNumber().trim())
                .firstNames(registration.getFirstNames().trim())
                .lastNames(registeredLastNames(registration.getLastNames()))
                .phone(TextNormalizer.trim(registration.getPhone()))
                .email(email)
                .passwordHash(passwordHasher.encode(registration.getPassword()))
                .role(PersonRole.CUSTOMER.toStorageValue())
                .status(true)
                .lastLogin(registeredAt)
                .createdAt(registeredAt)
                .customer(registeredCustomer(registration, registeredAt))
                .build();
    }

    private CustomerEmbedded registeredCustomer(RegisterRequestDTO registration, LocalDateTime registeredAt) {
        return CustomerEmbedded.builder()
                .address(Objects.requireNonNullElse(TextNormalizer.trim(registration.getAddress()),
                        DEFAULT_CUSTOMER_ADDRESS))
                .geographicLocationId(registrationLocation(registration.getGeographicLocationId()))
                .status(true)
                .createdAt(registeredAt)
                .build();
    }

    // Self-registration has no RUC check, so blank last names get the placeholder.
    private String registeredLastNames(String lastNames) {
        return TextNormalizer.isBlank(lastNames) ? AppConstants.MISSING_VALUE_PLACEHOLDER : lastNames.trim();
    }

    static Integer registrationLocation(Integer geographicLocationId) {
        return Objects.requireNonNullElse(geographicLocationId, PersonValidator.DEFAULT_GEOGRAPHIC_LOCATION_ID);
    }

    private String initialPassword(String password) {
        return TextNormalizer.isBlank(password) ? DEFAULT_PERSON_PASSWORD : password;
    }

    private String storageRole(PersonRole role) {
        return role == null ? null : role.toStorageValue();
    }

    private CustomerEmbedded toCustomerEmbedded(CustomerDTO customer) {
        if (customer == null) {
            return null;
        }
        return CustomerEmbedded.builder()
                .address(customer.getAddress())
                .geographicLocationId(customer.getGeographicLocationId())
                .status(customer.getStatus() == null || customer.getStatus())
                .createdAt(Objects.requireNonNullElseGet(customer.getCreatedAt(), LocalDateTime::now))
                .updatedAt(customer.getUpdatedAt())
                .deletedAt(customer.getDeletedAt())
                .restoredAt(customer.getRestoredAt())
                .build();
    }
}
