package pe.edu.vallegrande.farmagro.model.sql;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("SALE_DETAILS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleDetail {

    @Id
    @Column("id_sale_detail")
    private Integer idSaleDetail;

    @Column("quantity")
    private Integer quantity;

    @Column("unit_price")
    private BigDecimal unitPrice;

    @Column("item_discount")
    private BigDecimal itemDiscount;

    @Column("sales_id_sale")
    private Integer saleId;

    @Column("mongo_id_product")
    private String mongoIdProduct;

    @Column("mongo_id_presentation")
    private String mongoIdPresentation;

    // Batch selection follows FEFO (first-expired, first-out) inventory rules.
    @Column("mongo_id_batch")
    private String mongoIdBatch;
}
