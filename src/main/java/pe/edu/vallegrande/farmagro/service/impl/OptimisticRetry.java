package pe.edu.vallegrande.farmagro.service.impl;

import org.springframework.dao.OptimisticLockingFailureException;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;

/** Retry for read-modify-write of versioned documents: re-read and apply again on a version conflict. */
final class OptimisticRetry {

    private static final int MAX_RETRIES = 5;
    private static final Duration FIRST_BACKOFF = Duration.ofMillis(20);

    private OptimisticRetry() {
    }

    static RetryBackoffSpec onVersionConflict() {
        return Retry.backoff(MAX_RETRIES, FIRST_BACKOFF)
                .filter(OptimisticLockingFailureException.class::isInstance)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }
}
