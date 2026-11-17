package pe.edu.vallegrande.farmagro.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

@Slf4j
final class BusinessIdSequence<T> {

    private static final int NO_EXISTING_SEQUENCE = 0;
    private static final String SEQUENCE_FORMAT = "%0" + AppConstants.MIN_ID_DIGITS + "d";
    private static final String STORED_SEQUENCE_PATTERN = "[0-9]+$";
    private static final String INVALID_ID_MESSAGE = "El id de %s debe tener el formato %s seguido de %d a %d dígitos (ej: %s)";
    private static final String DUPLICATE_ID_MESSAGE = "Ya existe un registro de %s con el id %s";
    private static final int MAX_INSERT_ATTEMPTS = 10;
    // Random pause before a retry so simultaneous creations stop recomputing the same ID.
    private static final int RETRY_JITTER_MAX_MILLIS = 40;
    private static final int RETRY_SKIP_FACTOR = 3;
    private static final String ID_INDEX_NAME = "_id_";
    private static final String EXHAUSTED_SEQUENCE_MESSAGE = "No quedan ids de %s disponibles (máximo 10 caracteres)";

    record IdFormat(String prefix, String entityName, String inputIdPattern) {
    }

    private final String prefix;
    private final String entityName;
    private final String inputIdPattern;
    private final String storedIdPattern;
    private final ReactiveMongoRepository<T, String> repository;
    private final Function<T, String> idReader;

    BusinessIdSequence(IdFormat idFormat, ReactiveMongoRepository<T, String> repository, Function<T, String> idReader) {
        this.prefix = idFormat.prefix();
        this.entityName = idFormat.entityName();
        this.inputIdPattern = idFormat.inputIdPattern();
        this.storedIdPattern = "^" + prefix + STORED_SEQUENCE_PATTERN;
        this.repository = repository;
        this.idReader = idReader;
    }

    /**
     * Inserts a new document with the provided ID or the next free one. {@code insert} fails on an
     * existing _id instead of replacing the stored document (what {@code save} does), so two
     * concurrent creations can no longer end up sharing one ID; the loser retries with a fresh ID.
     */
    Mono<T> insertNew(String providedId, Function<String, T> entityFactory) {
        if (providedId != null && !providedId.isBlank()) {
            return validateProvidedId(providedId)
                    .flatMap(id -> repository.insert(entityFactory.apply(id)))
                    .onErrorMap(this::isIdCollision, error -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            DUPLICATE_ID_MESSAGE.formatted(entityName, providedId)));
        }
        return insertWithNextId(entityFactory, 1);
    }

    // After a collision the candidate skips a random, growing offset so concurrent writers spread out.
    private Mono<T> insertWithNextId(Function<String, T> entityFactory, int attempt) {
        int skip = attempt == 1 ? 0 : ThreadLocalRandom.current().nextInt(attempt * RETRY_SKIP_FACTOR);
        return next(skip)
                .flatMap(id -> repository.insert(entityFactory.apply(id)))
                .onErrorResume(error -> isIdCollision(error) && attempt < MAX_INSERT_ATTEMPTS,
                        error -> Mono.delay(Duration.ofMillis(ThreadLocalRandom.current().nextInt(1, RETRY_JITTER_MAX_MILLIS)))
                                .then(insertWithNextId(entityFactory, attempt + 1)));
    }

    private boolean isIdCollision(Throwable error) {
        return error instanceof DuplicateKeyException && String.valueOf(error.getMessage()).contains(ID_INDEX_NAME);
    }

    // A full scan is fine for small collections; concurrent inserts are resolved by insertNew.
    private Mono<String> next(int skip) {
        return repository.findAll()
                .map(idReader)
                .filter(storedId -> storedId != null && storedId.matches(storedIdPattern))
                .map(this::parseSequence)
                .reduce(NO_EXISTING_SEQUENCE, Math::max)
                .flatMap(highestSequence -> nextAvailable(highestSequence + AppConstants.ID_SEQUENCE_STEP + skip));
    }

    private Mono<String> validateProvidedId(String providedId) {
        if (!providedId.matches(inputIdPattern)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_ID_MESSAGE.formatted(
                    entityName, prefix, AppConstants.MIN_ID_DIGITS, AppConstants.MAX_ID_DIGITS,
                    format(AppConstants.ID_SEQUENCE_STEP))));
        }
        return repository.existsById(providedId)
                .flatMap(exists -> exists
                        ? Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                DUPLICATE_ID_MESSAGE.formatted(entityName, providedId)))
                        : Mono.just(providedId));
    }

    private Mono<String> nextAvailable(int sequence) {
        if (sequence > AppConstants.MAX_ID_SEQUENCE) {
            return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
                    EXHAUSTED_SEQUENCE_MESSAGE.formatted(entityName)));
        }
        String candidateId = format(sequence);
        return repository.existsById(candidateId)
                .flatMap(exists -> exists
                        ? nextAvailable(sequence + AppConstants.ID_SEQUENCE_STEP)
                        : Mono.just(candidateId));
    }

    private String format(int sequence) {
        return prefix + SEQUENCE_FORMAT.formatted(sequence);
    }

    private int parseSequence(String storedId) {
        try {
            return Integer.parseInt(storedId.substring(prefix.length()));
        } catch (NumberFormatException numberFormatException) {
            log.warn("Ignoring {} ID '{}' because its sequence exceeds the integer range", entityName, storedId);
            return NO_EXISTING_SEQUENCE;
        }
    }
}
