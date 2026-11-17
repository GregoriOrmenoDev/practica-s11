package pe.edu.vallegrande.farmagro.service.impl;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.stream.Collectors;

final class ImportDtoValidation {

    private ImportDtoValidation() {
    }

    static <T> Mono<T> validate(Validator validator, T value) {
        String violations = validator.validate(value).stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(ImportDtoValidation::formatViolation)
                .collect(Collectors.joining("; "));
        if (violations.isEmpty()) {
            return Mono.just(value);
        }
        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, violations));
    }

    // Messages already name the field in Spanish; the internal property path is left out.
    private static String formatViolation(ConstraintViolation<?> violation) {
        return violation.getMessage();
    }

    /** User-facing reason of a failed row: the status reason when present, never the HTTP prefix. */
    static String reason(Throwable error) {
        return error instanceof ResponseStatusException statusException && statusException.getReason() != null
                ? statusException.getReason() : error.getMessage();
    }
}
