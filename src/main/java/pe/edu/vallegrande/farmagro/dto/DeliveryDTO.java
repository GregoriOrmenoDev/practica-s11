package pe.edu.vallegrande.farmagro.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryDTO {

    private Integer idDelivery;

    @NotBlank(message = "El tipo de entrega es obligatorio")
    @Pattern(regexp = "^(Delivery|Pickup|Courier)$",
            message = "El tipo de entrega debe ser: Delivery, Pickup o Courier")
    private String deliveryType;

    @NotBlank(message = "La referencia es obligatoria")
    @Size(max = 150, message = "La referencia no puede superar los 150 caracteres")
    private String reference;

    @Size(max = 400, message = "La dirección no puede superar los 400 caracteres")
    private String deliveryAddress;

    @NotNull(message = "El id de geographicLocation es obligatorio")
    @JsonProperty("ubigeoId")
    @JsonAlias("geographicLocationId")
    private Integer geographicLocationId;

    @NotNull(message = "La fecha programada es obligatoria")
    private LocalDateTime scheduledDate;

    private LocalDateTime actualDate;

    @Pattern(regexp = "^(DELIVERED|PENDING|ON_THE_WAY|CANCELLED)$",
            message = "El estado debe ser: DELIVERED, PENDING, ON_THE_WAY o CANCELLED")
    private String status;

    @Size(max = 250, message = "Las notas no pueden superar los 250 caracteres")
    private String notes;

    @NotNull(message = "El id de venta es obligatorio")
    private Integer saleId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime restoredAt;
}
