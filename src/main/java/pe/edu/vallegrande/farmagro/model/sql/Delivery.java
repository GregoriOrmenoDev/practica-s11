package pe.edu.vallegrande.farmagro.model.sql;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("DELIVERIES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @Column("id_delivery")
    private Integer idDelivery;

    @Column("delivery_type")
    private String deliveryType;

    @Column("reference")
    private String reference;

    @Column("delivery_address")
    private String deliveryAddress;

    @Column("ubigeo_id_ubigeo")
    private Integer geographicLocationId;

    @Column("scheduled_date")
    private LocalDateTime scheduledDate;

    @Column("actual_date")
    private LocalDateTime actualDate;

    @Column("status")
    private String status;

    @Column("notes")
    private String notes;

    @Column("sales_id_sale")
    private Integer saleId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("deleted_at")
    private LocalDateTime deletedAt;

    @Column("restored_at")
    private LocalDateTime restoredAt;
}
