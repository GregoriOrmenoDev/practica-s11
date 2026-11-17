package pe.edu.vallegrande.farmagro.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {

    @NotBlank(message = "La dirección es obligatoria")
    private String address;

    @NotNull(message = "El ubigeo es obligatorio")
    @Min(value = AppConstants.MINIMUM_ID, message = "El ubigeo debe ser mayor que 0")
    @JsonProperty("ubigeoId")
    @JsonAlias("geographicLocationId")
    private Integer geographicLocationId;

    private Boolean status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime restoredAt;
}
