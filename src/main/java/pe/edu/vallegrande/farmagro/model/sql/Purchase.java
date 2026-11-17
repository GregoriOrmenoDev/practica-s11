package pe.edu.vallegrande.farmagro.model.sql;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("PURCHASES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Purchase {

    @Id
    @Column("id_purchase")
    private Integer idPurchase;

    @Column("purchase_date")
    private LocalDateTime purchaseDate;

    @Column("invoice_number")
    private String invoiceNumber;

    @Column("expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;

    @Column("subtotal")
    private BigDecimal subtotal;

    @Column("igv")
    private BigDecimal igv;

    @Column("total")
    private BigDecimal total;

    @Column("status")
    private String status;

    @Column("notes")
    private String notes;

    @Column("mongo_id_supplier")
    private String mongoIdSupplier;

    @Column("mongo_id_user")
    private String mongoIdUser;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("deleted_at")
    private LocalDateTime deletedAt;

    @Column("restored_at")
    private LocalDateTime restoredAt;
}
