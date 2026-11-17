package pe.edu.vallegrande.farmagro.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseDetailDTO {

    private Integer idPurchaseDetails;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = AppConstants.MINIMUM_QUANTITY,
            message = "La cantidad debe ser al menos " + AppConstants.MINIMUM_QUANTITY)
    private Integer quantity;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = AppConstants.MINIMUM_POSITIVE_AMOUNT, message = "El precio unitario debe ser mayor que 0")
    private BigDecimal unitPrice;

    @NotNull(message = "El subtotal es obligatorio")
    @DecimalMin(value = AppConstants.MINIMUM_POSITIVE_AMOUNT, message = "El subtotal debe ser mayor a 0")
    private BigDecimal subtotal;

    private Integer purchasesIdPurchase;

    @NotBlank(message = "El id del producto es obligatorio")
    @Pattern(regexp = AppConstants.PRODUCT_ID_PATTERN, message = "El id del producto debe tener el formato PRD seguido de 3 a 7 dígitos")
    private String mongoIdProduct;

    @NotBlank(message = "El id de la presentación es obligatorio")
    @Pattern(regexp = AppConstants.PRESENTATION_ID_PATTERN, message = "El id de la presentación debe tener el formato PST seguido de 3 a 7 dígitos")
    private String mongoIdPresentation;

    // An existing batch, or none when the line brings a new batch (newBatch).
    @Pattern(regexp = AppConstants.BATCH_ID_PATTERN, message = "El id del lote debe tener el formato LOT seguido de 3 a 7 dígitos")
    private String mongoIdBatch;

    @Valid
    private NewBatchDTO newBatch;
}
