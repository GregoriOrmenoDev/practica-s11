package pe.edu.vallegrande.farmagro.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SupplierDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid SupplierDTO passes all validations")
    void validSupplierDtoShouldPassValidation() {
        SupplierDTO dto = SupplierDTO.builder()
                .documentType("RUC")
                .documentNumber("20601234567")
                .businessName("Agroquímicos del Norte SAC")
                .phone("987654321")
                .email("contacto@agroNorte.com")
                .address("Av. Los Pinos 123, Lima")
                .geographicLocationId(150101)
                .build();

        Set<ConstraintViolation<SupplierDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Valid DTO should have no violations");
    }

    @Test
    @DisplayName("SupplierDTO with invalid document type fails validation")
    void invalidDocTypeShouldFailValidation() {
        SupplierDTO dto = SupplierDTO.builder()
                .documentType("PASAPORTE")
                .documentNumber("20601234567")
                .businessName("Agroquímicos del Norte SAC")
                .phone("987654321")
                .email("contacto@agroNorte.com")
                .address("Av. Los Pinos 123, Lima")
                .geographicLocationId(150101)
                .build();

        Set<ConstraintViolation<SupplierDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Invalid document type should trigger violation");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("documentType")));
    }

    @Test
    @DisplayName("SupplierDTO with invalid phone format fails validation")
    void invalidPhoneFormatShouldFailValidation() {
        SupplierDTO dto = SupplierDTO.builder()
                .documentType("RUC")
                .documentNumber("20601234567")
                .businessName("Agroquímicos del Norte SAC")
                .phone("12345")
                .email("contacto@agroNorte.com")
                .address("Av. Los Pinos 123, Lima")
                .geographicLocationId(150101)
                .build();

        Set<ConstraintViolation<SupplierDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Phone with less than 9 digits should trigger violation");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("phone")));
    }

    @Test
    @DisplayName("SupplierDTO with invalid email format fails validation")
    void invalidEmailShouldFailValidation() {
        SupplierDTO dto = SupplierDTO.builder()
                .documentType("DNI")
                .documentNumber("12345678")
                .businessName("Proveedor Individual")
                .phone("987654321")
                .email("not-an-email")
                .address("Jr. Las Rosas 456")
                .geographicLocationId(150101)
                .build();

        Set<ConstraintViolation<SupplierDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Invalid email should trigger violation");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }
}
