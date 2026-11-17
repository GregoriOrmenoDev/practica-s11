package pe.edu.vallegrande.farmagro.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseDTO {

    private Integer idPurchase;

    private LocalDateTime purchaseDate;

    @NotBlank(message = "El número de factura es obligatorio")
    @Size(max = 50, message = "El número de factura no puede superar los 50 caracteres")
    private String invoiceNumber;

    private LocalDateTime expectedDeliveryDate;

    @NotNull(message = "El subtotal es obligatorio")
    @DecimalMin(value = AppConstants.MINIMUM_POSITIVE_AMOUNT, message = "El subtotal debe ser mayor a 0")
    private BigDecimal subtotal;

    @NotNull(message = "El IGV es obligatorio")
    @DecimalMin(value = AppConstants.MINIMUM_NON_NEGATIVE_AMOUNT, message = "El IGV no puede ser negativo")
    private BigDecimal igv;

    @NotNull(message = "El total es obligatorio")
    @DecimalMin(value = AppConstants.MINIMUM_POSITIVE_AMOUNT, message = "El total debe ser mayor a 0")
    private BigDecimal total;

    @Pattern(regexp = "^(PAID|PENDING|CANCELLED)$",
            message = "El estado debe ser: PAID, PENDING o CANCELLED")
    private String status;

    @Size(max = 300, message = "Las notas no pueden superar los 300 caracteres")
    private String notes;

    @NotBlank(message = "El id del proveedor es obligatorio")
    @Pattern(regexp = AppConstants.SUPPLIER_ID_PATTERN, message = "El id del proveedor debe tener el formato SUP seguido de 3 a 7 dígitos")
    private String mongoIdSupplier;

    @NotBlank(message = "El id del usuario es obligatorio")
    @Pattern(regexp = AppConstants.PERSON_ID_PATTERN, message = "El id de la persona debe tener el formato PER seguido de 3 a 7 dígitos")
    private String mongoIdUser;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime restoredAt;

    @NotNull(message = "Los detalles de compra son obligatorios")
    @Size(min = AppConstants.MINIMUM_ITEMS, message = "La compra debe tener al menos un detalle")
    @Valid
    private List<PurchaseDetailDTO> details;
}
