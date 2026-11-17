package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.dto.SupplierDTO;
import pe.edu.vallegrande.farmagro.model.Supplier;
import org.springframework.stereotype.Component;

@Component
final class SupplierMapper {

    SupplierDTO toDTO(Supplier supplier) {
        return SupplierDTO.builder()
                .id(supplier.getId())
                .documentType(supplier.getDocumentType())
                .documentNumber(supplier.getDocumentNumber())
                .businessName(supplier.getBusinessName())
                .contactName(supplier.getContactName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .geographicLocationId(supplier.getGeographicLocationId())
                .productCategories(supplier.getProductCategories())
                .certifications(supplier.getCertifications())
                .status(supplier.getStatus())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .deletedAt(supplier.getDeletedAt())
                .restoredAt(supplier.getRestoredAt())
                .build();
    }
}
