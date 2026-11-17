package pe.edu.vallegrande.farmagro.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Set;

public final class StatusFilters {

    public static final Set<String> ORDER_STATUSES = Set.of(
            AppConstants.STATUS_PAID, AppConstants.STATUS_PENDING, AppConstants.STATUS_CANCELLED);
    public static final String ORDER_STATUSES_MESSAGE = "PAID, PENDING, CANCELLED";

    public static final Set<String> DELIVERY_STATUSES = Set.of(
            AppConstants.STATUS_DELIVERED, AppConstants.STATUS_PENDING,
            AppConstants.STATUS_ON_THE_WAY, AppConstants.STATUS_CANCELLED);
    public static final String DELIVERY_STATUSES_MESSAGE = "DELIVERED, PENDING, ON_THE_WAY, CANCELLED";

    private StatusFilters() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Mono<String> validateOrderStatus(String status) {
        return validate(status, ORDER_STATUSES, ORDER_STATUSES_MESSAGE);
    }

    public static Mono<String> validateDeliveryStatus(String status) {
        return validate(status, DELIVERY_STATUSES, DELIVERY_STATUSES_MESSAGE);
    }

    private static Mono<String> validate(String status, Set<String> allowedStatuses, String allowedMessage) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!allowedStatuses.contains(normalizedStatus)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Estado inválido: '" + status + "'. Valores permitidos: " + allowedMessage));
        }
        return Mono.just(normalizedStatus);
    }
}
