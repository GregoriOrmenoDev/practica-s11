package pe.edu.vallegrande.farmagro.model.sql;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("SALES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    @Id
    @Column("id_sale")
    private Integer idSale;

    @Column("sale_date")
    private LocalDateTime saleDate;

    @Column("invoice_number")
    private String invoiceNumber;

    @Column("voucher_type")
    private String voucherType;

    @Column("payment_method")
    private String paymentMethod;

    @Column("subtotal")
    private BigDecimal subtotal;

    @Column("discount")
    private BigDecimal discount;

    @Column("igv")
    private BigDecimal igv;

    @Column("total")
    private BigDecimal total;

    @Column("status")
    private String status;

    @Column("notes")
    private String notes;

    @Column("sales_channel")
    private String salesChannel;

    @Column("mongo_id_customer")
    private String mongoIdCustomer;

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
