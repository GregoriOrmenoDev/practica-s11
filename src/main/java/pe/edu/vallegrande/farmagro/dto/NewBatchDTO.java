package pe.edu.vallegrande.farmagro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Batch received with a purchase line; the backend creates it in the presentation and assigns its LOT id. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewBatchDTO {

    @NotBlank(message = "El número del lote nuevo es obligatorio")
    @Size(max = 50, message = "El número del lote no puede superar los 50 caracteres")
    private String batchNumber;

    private LocalDate manufactureDate;

    @NotNull(message = "La fecha de vencimiento del lote nuevo es obligatoria")
    private LocalDate expirationDate;
}
