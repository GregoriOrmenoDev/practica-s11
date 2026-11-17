package pe.edu.vallegrande.farmagro.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** Shared existence and status checks for the master records (person, product, supplier). */
public final class ValidationUtils {

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /** `entityLabel` is the Spanish noun shown to the user, e.g. "persona". */
    public static <T> Mono<T> validateEntityExists(Mono<T> entityMono, String entityLabel, Object id) {
        return entityMono.switchIfEmpty(Mono.error(new ResponseStatusException(
                HttpStatus.NOT_FOUND, "No existe un registro de " + entityLabel + " con id " + id)));
    }

    public static Mono<Void> validateCanDelete(Boolean isActive, String entityLabel) {
        if (Boolean.FALSE.equals(isActive)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El registro de " + entityLabel + " ya está inactivo"));
        }
        return Mono.empty();
    }

    public static Mono<Void> validateCanRestore(Boolean isActive, String entityLabel) {
        if (Boolean.TRUE.equals(isActive)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El registro de " + entityLabel + " ya está activo"));
        }
        return Mono.empty();
    }
}
