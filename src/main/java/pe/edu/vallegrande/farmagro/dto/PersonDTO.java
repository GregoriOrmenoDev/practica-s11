package pe.edu.vallegrande.farmagro.dto;

import pe.edu.vallegrande.farmagro.util.AppConstants;

import jakarta.validation.Valid;
import pe.edu.vallegrande.farmagro.model.PersonRole;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonDTO {

    @Pattern(regexp = AppConstants.PERSON_ID_PATTERN,
            message = "El id debe tener el formato PER seguido de al menos 3 dígitos (ej: PER001)")
    private String id;

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "^(DNI|CE|RUC|PAS)$",
            message = "El tipo de documento debe ser DNI, CE, RUC o PAS")
    private String documentType;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 12, message = "El número de documento no puede superar los 12 caracteres")
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

    @Size(max = 255)
    private String passwordHash;

    // JSON accepts legacy role codes and returns the full English role names.
    @NotNull(message = "El rol es obligatorio")
    private PersonRole role;

    private String avatarUrl;

    private Boolean status;

    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime restoredAt;

    @Valid
    private CustomerDTO customer;
}
