package pe.edu.vallegrande.farmagro.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class SaleDTO {

    private Integer idSale;

    private LocalDateTime saleDate;

    // Optional on creation: when omitted the backend assigns the next correlative of the series.
    @Pattern(regexp = "^[A-Za-z0-9]{2,5}-[0-9]{1,8}$",
            message = "El número de comprobante debe tener formato de serie y correlativo (ej: B001-0521 o F001-0001)")
    private String invoiceNumber;

    // Invoices require a RUC customer; DNI/CE customers get receipts.
    @Pattern(regexp = "^(RECEIPT|INVOICE)$",
            message = "El tipo de comprobante debe ser RECEIPT o INVOICE")
    private String voucherType;

    @NotBlank(message = "El método de pago es obligatorio")
    @Pattern(regexp = "^(Cash|Transfer|Yape|Plin)$",
            message = "El método de pago debe ser Cash, Transfer, Yape o Plin")
    private String paymentMethod;

    @NotNull(message = "El subtotal es obligatorio")
    @DecimalMin(value = AppConstants.MINIMUM_POSITIVE_AMOUNT, message = "El subtotal debe ser mayor que 0")
    private BigDecimal subtotal;

    @DecimalMin(value = AppConstants.MINIMUM_NON_NEGATIVE_AMOUNT, message = "El descuento no puede ser negativo")
    private BigDecimal discount;

    @DecimalMin(value = AppConstants.MINIMUM_NON_NEGATIVE_AMOUNT, message = "El IGV no puede ser negativo")
    private BigDecimal igv;

    @NotNull(message = "El total es obligatorio")
    @DecimalMin(value = AppConstants.MINIMUM_POSITIVE_AMOUNT, message = "El total debe ser mayor que 0")
    private BigDecimal total;

    @Pattern(regexp = "^(PAID|PENDING|CANCELLED)$",
            message = "El estado debe ser PAID, PENDING o CANCELLED")
    private String status;

    @Size(max = 200, message = "Las notas no pueden superar los 200 caracteres")
    private String notes;

    @NotBlank(message = "El canal de venta es obligatorio")
    @Pattern(regexp = "^(Store|WhatsApp|Web|Phone|Other)$",
            message = "El canal debe ser Store, WhatsApp, Web, Phone u Other")
    private String salesChannel;

    @NotBlank(message = "El id del cliente es obligatorio")
    @Pattern(regexp = AppConstants.PERSON_ID_PATTERN,
            message = "El id del cliente debe tener el formato PER seguido de al menos 3 dígitos (ej: PER001)")
    private String mongoIdCustomer;

    @NotBlank(message = "El id del vendedor es obligatorio")
    @Pattern(regexp = AppConstants.PERSON_ID_PATTERN,
            message = "El id del vendedor debe tener el formato PER seguido de al menos 3 dígitos (ej: PER001)")
    private String mongoIdUser;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime restoredAt;

    @NotNull(message = "Los detalles de venta son obligatorios")
    @Size(min = AppConstants.MINIMUM_ITEMS, message = "La venta debe tener al menos un detalle")
    @Valid
    private List<SaleDetailDTO> details;

    /** Optional delivery created with the sale (online orders); not returned in responses. */
    @Valid
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private SaleDeliveryDTO delivery;
}
