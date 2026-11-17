package pe.edu.vallegrande.farmagro.model.sql;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("PURCHASE_DETAILS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseDetail {

    @Id
    @Column("id_purchase_details")
    private Integer idPurchaseDetails;

    @Column("quantity")
    private Integer quantity;

    @Column("unit_price")
    private BigDecimal unitPrice;

    @Column("subtotal")
    private BigDecimal subtotal;

    @Column("purchases_id_purchase")
    private Integer purchasesIdPurchase;

    @Column("mongo_id_product")
    private String mongoIdProduct;

    @Column("mongo_id_presentation")
    private String mongoIdPresentation;

    @Column("mongo_id_batch")
    private String mongoIdBatch;
}
