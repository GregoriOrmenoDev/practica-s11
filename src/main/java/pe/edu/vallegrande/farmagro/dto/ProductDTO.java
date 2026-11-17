package pe.edu.vallegrande.farmagro.dto;

import jakarta.validation.constraints.*;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {

    
    @Pattern(regexp = AppConstants.PRODUCT_ID_PATTERN, message = "El id debe tener el formato PRD seguido de 3 a 7 dígitos (ej: PRD001)")
    private String id;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 150, message = "El nombre del producto no puede superar los 150 caracteres")
    private String name;

    @NotBlank(message = "La marca es obligatoria")
    @Size(max = 150, message = "La marca no puede superar los 150 caracteres")
    private String brand;

    @Size(max = 100, message = "El registro sanitario no puede superar los 100 caracteres")
    private String sanitaryRegistration;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(min = AppConstants.MINIMUM_DESCRIPTION_LENGTH, max = AppConstants.MAXIMUM_DESCRIPTION_LENGTH,
            message = "La descripción debe tener entre 10 y 300 caracteres")
    private String description;

    @Size(max = 200, message = "El ingrediente activo no puede superar los 200 caracteres")
    private String activeIngredient;

    
    @Pattern(regexp = "^(I|II|III|IV)$", message = "El nivel de toxicidad debe ser I, II, III o IV")
    private String toxicityLevel;

    private Boolean requiresLicense;

    
    @NotNull(message = "Las categorías son obligatorias")
    @Size(min = AppConstants.MINIMUM_ITEMS, message = "Debe indicar al menos una categoría")
    private List<String> categories;

    @Size(max = 255, message = "La URL de la imagen no puede superar los 255 caracteres")
    private String imageUrl;

    
    private Map<String, Object> technicalSpecifications;

    
    private List<Object> presentations;

    
    private Double price;

    private String skuCode;

    
    private Boolean status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime restoredAt;
}
