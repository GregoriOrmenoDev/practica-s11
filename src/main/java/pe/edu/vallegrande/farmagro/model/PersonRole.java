package pe.edu.vallegrande.farmagro.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PersonRole {
    ADMINISTRATOR,
    VENDOR,
    WAREHOUSE,
    DELIVERY,
    CUSTOMER;

    @JsonCreator
    public static PersonRole fromValue(String roleCode) {
        if (roleCode == null) {
            return null;
        }
        return switch (roleCode.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "ADMIN", "ADMINISTRATOR" -> ADMINISTRATOR;
            case "VEND", "VENDOR" -> VENDOR;
            case "ALMAC", "WAREHOUSE" -> WAREHOUSE;
            case "DELIV", "DELIVERY" -> DELIVERY;
            case "CLIENT", "CUSTOMER" -> CUSTOMER;
            default -> throw new IllegalArgumentException("Rol de persona desconocido: " + roleCode);
        };
    }

    @JsonValue
    public String toValue() {
        return toStorageValue();
    }

    public String toStorageValue() {
        return switch (this) {
            case ADMINISTRATOR -> "ADMIN";
            case VENDOR -> "VEND";
            case WAREHOUSE -> "ALMAC";
            case DELIVERY -> "DELIV";
            case CUSTOMER -> "CLIENT";
        };
    }
}
