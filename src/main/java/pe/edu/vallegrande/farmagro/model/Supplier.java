package pe.edu.vallegrande.farmagro.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "suppliers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    
    @Id
    private String id;

    @Field("doc_type")
    private String documentType;

    @Field("doc_number")
    private String documentNumber;

    
    @Field("company_name")
    private String businessName;

    
    @Field("contact_name")
    private String contactName;

    @Field("phone")
    private String phone;

    @Indexed(unique = true)
    @Field("email")
    private String email;

    @Field("address")
    private String address;

    
    @Field("ubigeo_id")
    private Integer geographicLocationId;

    
    @Field("product_categories")
    private List<String> productCategories;

    
    @Field("certifications")
    private Map<String, Object> certifications;

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
