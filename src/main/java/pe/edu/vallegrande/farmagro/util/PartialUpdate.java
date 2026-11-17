package pe.edu.vallegrande.farmagro.util;

import java.util.function.Consumer;

/**
 * PUT requests only overwrite the fields they actually send: a null value keeps the stored one,
 * so a client that omits a field (or an older client) can no longer erase data by accident.
 * To clear an optional text field, clients send an empty string.
 */
public final class PartialUpdate {

    private PartialUpdate() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static <T> void setIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
