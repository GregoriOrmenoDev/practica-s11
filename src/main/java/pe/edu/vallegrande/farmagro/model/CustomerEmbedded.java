package pe.edu.vallegrande.farmagro.model;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEmbedded {

    @Field("address")
    private String address;

    // Flat reference to UBIGEO in SQL Server (looked up in the backend).
    @Field("ubigeo_id")
    private Integer geographicLocationId;

    @Field("status")
    private Boolean status;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    private LocalDateTime deletedAt;

    @Field("restored_at")
    private LocalDateTime restoredAt;
}
