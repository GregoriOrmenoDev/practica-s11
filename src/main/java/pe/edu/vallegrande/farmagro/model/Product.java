package pe.edu.vallegrande.farmagro.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product implements Persistable<String> {

    @Id
    private String id;

    // Optimistic locking: stock is read-modify-written, so a save based on a stale copy fails and the
    // caller retries on fresh data instead of overwriting a concurrent sale, purchase or edit.
    @Version
    private Long version;

    @Field("name")
    @Indexed(unique = true)
    private String name;

    @Field("brand")
    private String brand;

    @Field("health_registration")
    @Indexed(unique = true)
    private String sanitaryRegistration;

    @Field("description")
    private String description;

    @Field("active_ingredient")
    private String activeIngredient;

    
    @Field("toxicity_level")
    private String toxicityLevel;

    @Field("requires_license")
    private Boolean requiresLicense;

    
    @Field("categories")
    private List<String> categories;

    @Field("image_url")
    private String imageUrl;

    
    @Field("technical_specs")
    private Map<String, Object> technicalSpecifications;

    
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

    
    @Field("presentations")
    private List<Object> presentations;

    // Products are always created with insert() (BusinessIdSequence), so save() is only an update.
    // Documents stored before the version field existed have none and match the first versioned save.
    @Override
    @Transient
    public boolean isNew() {
        return false;
    }
}
