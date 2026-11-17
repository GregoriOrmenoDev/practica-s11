package pe.edu.vallegrande.farmagro.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Delivery requested together with a sale (online orders): the backend creates it PENDING in the same
 * transaction as the sale, so an order never exists without the delivery the customer asked for.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleDeliveryDTO {

    @NotBlank(message = "El tipo de entrega es obligatorio")
    @Pattern(regexp = "^(Delivery|Pickup)$", message = "El tipo de entrega del pedido debe ser Delivery o Pickup")
    private String deliveryType;

    @Size(max = 400, message = "La dirección no puede superar los 400 caracteres")
    private String deliveryAddress;

    @Size(max = 150, message = "La referencia no puede superar los 150 caracteres")
    private String reference;

    @NotNull(message = "El distrito (ubigeo) de la entrega es obligatorio")
    @JsonProperty("ubigeoId")
    @JsonAlias("geographicLocationId")
    private Integer geographicLocationId;

    /** Optional: by default delivery is scheduled two days ahead and pickup one day ahead, at 09:00 (Lima). */
    private LocalDateTime scheduledDate;
}
