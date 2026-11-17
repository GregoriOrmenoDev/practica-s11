package pe.edu.vallegrande.farmagro.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeographicLocationDTO {

    @NotNull(message = "El id de geographicLocation es obligatorio")
    @JsonProperty("ubigeoId")
    @JsonAlias("geographicLocationId")
    private Integer geographicLocationId;

    @NotBlank(message = "El departamento es obligatorio")
    private String department;

    @NotBlank(message = "La provincia es obligatoria")
    private String province;

    @NotBlank(message = "El distrito es obligatorio")
    private String district;
}
