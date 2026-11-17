package pe.edu.vallegrande.farmagro.dto;

import pe.edu.vallegrande.farmagro.util.AppConstants;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierDTO {

    
    @Pattern(regexp = AppConstants.SUPPLIER_ID_PATTERN, message = "El id debe tener el formato SUP seguido de 3 a 7 dígitos (ej: SUP001)")
    private String id;

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "^(RUC|DNI|CE)$", message = "El tipo de documento debe ser RUC, DNI o CE")
    private String documentType;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 11, message = "El número de documento no puede superar los 11 caracteres")
    private String documentNumber;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 150, message = "La razón social no puede superar los 150 caracteres")
    private String businessName;

    @Size(max = 200, message = "El nombre de contacto no puede superar los 200 caracteres")
    private String contactName;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^\\d{9}$", message = "El teléfono debe tener exactamente 9 dígitos")
    private String phone;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 150)
    private String address;

    
    @NotNull(message = "El ubigeo es obligatorio")
    @JsonProperty("ubigeoId")
    @JsonAlias("geographicLocationId")
    private Integer geographicLocationId;

    
    private List<String> productCategories;

    
    private Map<String, Object> certifications;

    
    private Boolean status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime restoredAt;
}
