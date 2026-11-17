package pe.edu.vallegrande.farmagro.service.impl;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import pe.edu.vallegrande.farmagro.dto.CustomerDTO;
import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import pe.edu.vallegrande.farmagro.model.CustomerEmbedded;
import pe.edu.vallegrande.farmagro.model.Person;
import pe.edu.vallegrande.farmagro.model.PersonRole;

@Slf4j
@Component
final class PersonMapper {

    PersonDTO toDTO(Person person) {
        return PersonDTO.builder()
                .id(person.getId())
                .documentType(person.getDocumentType())
                .documentNumber(person.getDocumentNumber())
                .firstNames(person.getFirstNames())
                .lastNames(person.getLastNames())
                .phone(person.getPhone())
                .email(person.getEmail())
                .passwordHash(null)
                .role(toPersonRole(person.getRole()))
                .avatarUrl(person.getAvatarUrl())
                .status(person.getStatus())
                .lastLogin(person.getLastLogin())
                .createdAt(person.getCreatedAt())
                .updatedAt(person.getUpdatedAt())
                .deletedAt(person.getDeletedAt())
                .restoredAt(person.getRestoredAt())
                .customer(toCustomerDTO(person.getCustomer()))
                .build();
    }

    private PersonRole toPersonRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return PersonRole.fromValue(role);
        } catch (IllegalArgumentException illegalArgumentException) {
            log.warn("Invalid person role: {}", role);
            return null;
        }
    }

    private CustomerDTO toCustomerDTO(CustomerEmbedded customer) {
        if (customer == null) {
            return null;
        }
        return CustomerDTO.builder()
                .address(customer.getAddress())
                .geographicLocationId(customer.getGeographicLocationId())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .deletedAt(customer.getDeletedAt())
                .restoredAt(customer.getRestoredAt())
                .build();
    }
}
