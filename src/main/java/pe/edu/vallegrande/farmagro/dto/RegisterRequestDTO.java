package pe.edu.vallegrande.farmagro.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequestDTO {

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "^(DNI|CE|RUC|PAS)$", message = "El tipo de documento debe ser DNI, CE, RUC o PAS")
    private String documentType;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(min = 4, max = 12, message = "El número de documento debe tener entre 4 y 12 caracteres")
    private String documentNumber;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 150, message = "Los nombres no pueden superar los 150 caracteres")
    private String firstNames;

    @Size(max = 150, message = "Los apellidos no pueden superar los 150 caracteres")
    private String lastNames;

    @Pattern(regexp = "^[0-9]{9}$", message = "El teléfono debe tener 9 dígitos numéricos")
    private String phone;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 50, message = "La contraseña debe tener entre 6 y 50 caracteres")
    private String password;

    @Size(max = 250, message = "La dirección no puede superar los 250 caracteres")
    private String address;

    @JsonProperty("ubigeoId")
    @JsonAlias("geographicLocationId")
    private Integer geographicLocationId;
}
