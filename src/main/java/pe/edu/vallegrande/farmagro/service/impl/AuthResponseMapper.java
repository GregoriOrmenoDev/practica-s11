package pe.edu.vallegrande.farmagro.service.impl;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.farmagro.dto.AuthResponseDTO;
import pe.edu.vallegrande.farmagro.dto.CustomerDTO;
import pe.edu.vallegrande.farmagro.model.CustomerEmbedded;
import pe.edu.vallegrande.farmagro.model.Person;

import java.util.UUID;

@Component
final class AuthResponseMapper {

    AuthResponseDTO toAuthResponse(Person person) {
        return AuthResponseDTO.builder()
                .id(person.getId())
                .documentType(person.getDocumentType())
                .documentNumber(person.getDocumentNumber())
                .firstNames(person.getFirstNames())
                .lastNames(person.getLastNames())
                .email(person.getEmail())
                .phone(person.getPhone())
                .role(person.getRole())
                .status(person.getStatus())
                .avatarUrl(person.getAvatarUrl())
                .lastLogin(person.getLastLogin())
                .customer(toCustomerDTO(person.getCustomer()))
                .token(UUID.randomUUID().toString())
                .build();
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
                .build();
    }
}
