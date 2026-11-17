package pe.edu.vallegrande.farmagro.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "person")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {

    @Id
    private String id;

    @Field("doc_type")
    private String documentType;

    @Field("doc_number")
    private String documentNumber;

    @Field("first_names")
    private String firstNames;

    @Field("last_names")
    private String lastNames;

    @Field("phone")
    private String phone;

    @Indexed(unique = true)
    @Field("email")
    private String email;

    @Field("password_hash")
    private String passwordHash;

    // Legacy role codes are retained in storage for existing MongoDB data.
    @Field("role")
    private String role;

    @Field("avatar_url")
    private String avatarUrl;

    @Field("last_login")
    private LocalDateTime lastLogin;

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

    // Embedded so customer data shares the person's document and lifecycle.
    @Field("customer")
    private CustomerEmbedded customer;
}
