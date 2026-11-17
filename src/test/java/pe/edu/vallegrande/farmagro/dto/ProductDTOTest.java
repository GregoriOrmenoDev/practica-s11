package pe.edu.vallegrande.farmagro.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid ProductDTO passes all validations")
    void validProductDtoShouldPassValidation() {
        ProductDTO dto = ProductDTO.builder()
                .name("Herbicida Max")
                .brand("AgroChile")
                .description("Herbicida de amplio espectro para cultivos")
                .toxicityLevel("III")
                .categories(List.of("Herbicidas"))
                .build();

        Set<ConstraintViolation<ProductDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Valid DTO should have no violations");
    }

    @Test
    @DisplayName("ProductDTO with blank name fails validation")
    void blankNameShouldFailValidation() {
        ProductDTO dto = ProductDTO.builder()
                .name("")
                .brand("AgroChile")
                .description("Herbicida de amplio espectro para cultivos")
                .toxicityLevel("II")
                .categories(List.of("Herbicidas"))
                .build();

        Set<ConstraintViolation<ProductDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Blank name should trigger violation");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    @DisplayName("ProductDTO with invalid toxicity level fails validation")
    void invalidToxicityLevelShouldFailValidation() {
        ProductDTO dto = ProductDTO.builder()
                .name("Herbicida Max")
                .brand("AgroChile")
                .description("Herbicida de amplio espectro para cultivos")
                .toxicityLevel("V")
                .categories(List.of("Herbicidas"))
                .build();

        Set<ConstraintViolation<ProductDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Invalid toxicity level should trigger violation");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("toxicityLevel")));
    }

    @Test
    @DisplayName("ProductDTO with empty categories fails validation")
    void emptyCategoriesShouldFailValidation() {
        ProductDTO dto = ProductDTO.builder()
                .name("Herbicida Max")
                .brand("AgroChile")
                .description("Herbicida de amplio espectro para cultivos")
                .toxicityLevel("I")
                .categories(List.of())
                .build();

        Set<ConstraintViolation<ProductDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Empty categories should trigger violation");
    }
}
