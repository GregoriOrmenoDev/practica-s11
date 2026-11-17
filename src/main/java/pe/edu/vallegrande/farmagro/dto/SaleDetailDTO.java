package pe.edu.vallegrande.farmagro.dto;

import jakarta.validation.constraints.*;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleDetailDTO {

    private Integer idSaleDetail;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = AppConstants.MINIMUM_QUANTITY,
            message = "La cantidad debe ser al menos " + AppConstants.MINIMUM_QUANTITY)
    private Integer quantity;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = AppConstants.MINIMUM_POSITIVE_AMOUNT, message = "El precio unitario debe ser mayor que 0")
    private BigDecimal unitPrice;

    @DecimalMin(value = AppConstants.MINIMUM_NON_NEGATIVE_AMOUNT, message = "El descuento no puede ser negativo")
    private BigDecimal itemDiscount;

    private BigDecimal subtotal;

    private Integer saleId;

    @NotBlank(message = "El id del producto es obligatorio")
    @Pattern(regexp = AppConstants.PRODUCT_ID_PATTERN,
            message = "El id del producto debe tener el formato PRD seguido de al menos 3 dígitos (ej: PRD001)")
    private String mongoIdProduct;

    @NotBlank(message = "El id de la presentación es obligatorio")
    @Pattern(regexp = AppConstants.PRESENTATION_ID_PATTERN,
            message = "El id de la presentación debe tener el formato PST seguido de al menos 3 dígitos (ej: PST001)")
    private String mongoIdPresentation;

    // Optional: when omitted the backend picks the batch by FEFO (first expiring, still sellable).
    @Pattern(regexp = AppConstants.BATCH_ID_PATTERN,
            message = "El id del lote debe tener el formato LOT seguido de al menos 3 dígitos (ej: LOT001)")
    private String mongoIdBatch;
}
