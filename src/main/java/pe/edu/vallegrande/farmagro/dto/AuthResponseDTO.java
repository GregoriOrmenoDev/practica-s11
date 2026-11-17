package pe.edu.vallegrande.farmagro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {
    private String id;
    private String documentType;
    private String documentNumber;
    private String firstNames;
    private String lastNames;
    private String email;
    private String phone;
    private String role;
    private Boolean status;
    private String avatarUrl;
    private LocalDateTime lastLogin;
    private CustomerDTO customer;
    private String token;
}
