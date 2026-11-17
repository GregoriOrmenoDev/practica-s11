package pe.edu.vallegrande.farmagro.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(GlobalExceptionHandlerRest.HANDLER_ORDER)
public class GlobalExceptionHandlerRest implements ErrorWebExceptionHandler {

    // Order -2 runs before Spring Boot's default error handler (-1).
    static final int HANDLER_ORDER = -2;
    private static final String INTERNAL_ERROR_MESSAGE = "Error interno del servidor";
    private static final String DUPLICATE_VALUE_MESSAGE =
            "Ya existe un registro con ese valor único (por ejemplo, número de comprobante, registro sanitario o código de barras)";
    private static final String BUSY_MESSAGE =
            "Otra operación está usando los mismos datos en este momento; vuelva a intentarlo";
    private static final String INVALID_INPUT_MESSAGE =
            "La solicitud tiene datos con formato o tipo incorrecto, o le falta un parámetro obligatorio";
    private static final String INVALID_FIELD_VALUE_MESSAGE = "Valor no válido para el campo ";
    private static final String VALIDATION_ERROR_SEPARATOR = "; ";

    @Override
    public Mono<Void> handle(ServerWebExchange serverWebExchange, Throwable exception) {
        if (serverWebExchange.getResponse().isCommitted()) {
            log.warn("Response already committed, skipping error handling for {}",
                    serverWebExchange.getRequest().getPath());
            return Mono.error(exception);
        }
        HttpStatus status = determineHttpStatus(exception);
        String message = extractErrorMessage(exception, serverWebExchange, status);
        return writeErrorResponse(serverWebExchange, status, buildErrorResponse(serverWebExchange, status, message));
    }

    private HttpStatus determineHttpStatus(Throwable exception) {
        if (exception instanceof WebExchangeBindException || exception instanceof ServerWebInputException) {
            return HttpStatus.BAD_REQUEST;
        } else if (exception instanceof DataIntegrityViolationException
                || exception instanceof PessimisticLockingFailureException
                || exception instanceof OptimisticLockingFailureException) {
            return HttpStatus.CONFLICT;
        } else if (exception instanceof ResponseStatusException statusException) {
            return HttpStatus.valueOf(statusException.getStatusCode().value());
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    private String extractErrorMessage(Throwable exception, ServerWebExchange serverWebExchange, HttpStatus status) {
        if (exception instanceof WebExchangeBindException bindingException) {
            return buildValidationErrorMessage(bindingException);
        } else if (exception instanceof ServerWebInputException inputException) {
            // Spring's reason ("Failed to read HTTP message", "Type mismatch"...) is technical English: log it only.
            log.warn("Invalid input at {}: {}", serverWebExchange.getRequest().getPath(), inputException.getMessage());
            return INVALID_INPUT_MESSAGE;
        } else if (exception instanceof ResponseStatusException statusException) {
            String message = statusException.getReason() != null ? statusException.getReason() : statusException.getMessage();
            log.warn("HTTP status exception {} at {}: {}", status, serverWebExchange.getRequest().getPath(), message);
            return message;
        } else if (exception instanceof PessimisticLockingFailureException
                || exception instanceof OptimisticLockingFailureException) {
            log.warn("Lock conflict at {}: {}", serverWebExchange.getRequest().getPath(), exception.getMessage());
            return BUSY_MESSAGE;
        } else if (exception instanceof DataIntegrityViolationException) {
            log.warn("Data integrity conflict at {}: {}", serverWebExchange.getRequest().getPath(), exception.getMessage());
            return DUPLICATE_VALUE_MESSAGE;
        } else {
            log.error("Unhandled error at {}: ", serverWebExchange.getRequest().getPath(), exception);
            return INTERNAL_ERROR_MESSAGE;
        }
    }

    private String buildValidationErrorMessage(WebExchangeBindException bindingException) {
        return bindingException.getBindingResult()
                .getAllErrors()
                .stream()
                // The DTO messages already name the field in Spanish; a type conversion failure has no
                // custom message, so it names the field instead of showing Spring's English text.
                .map(error -> error instanceof FieldError fieldError && fieldError.isBindingFailure()
                        ? INVALID_FIELD_VALUE_MESSAGE + fieldError.getField()
                        : error.getDefaultMessage())
                .collect(Collectors.joining(VALIDATION_ERROR_SEPARATOR));
    }

    private String buildErrorResponse(ServerWebExchange serverWebExchange, HttpStatus status, String message) {
        String safeMessage = message != null ? message : INTERNAL_ERROR_MESSAGE;
        return """
                {
                  "timestamp": "%s",
                  "status": %d,
                  "error": "%s",
                  "message": "%s",
                  "path": "%s"
                }
                """.formatted(
                escapeJson(LocalDateTime.now().toString()),
                status.value(),
                escapeJson(status.getReasonPhrase()),
                escapeJson(safeMessage),
                escapeJson(serverWebExchange.getRequest().getPath().value())
        );
    }

    private String escapeJson(String rawText) {
        return rawText.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange serverWebExchange, HttpStatus status, String body) {
        serverWebExchange.getResponse().setStatusCode(status);
        serverWebExchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        DataBuffer buffer = serverWebExchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return serverWebExchange.getResponse().writeWith(Mono.just(buffer));
    }
}
