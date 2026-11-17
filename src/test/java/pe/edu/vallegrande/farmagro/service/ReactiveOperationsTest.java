package pe.edu.vallegrande.farmagro.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReactiveOperationsTest {

    @Test
    @DisplayName("Mono.just emits one element and completes")
    void monoShouldEmitSingleElement() {
        Mono<String> mono = Mono.just("farmagro");

        StepVerifier.create(mono)
                .expectNext("farmagro")
                .verifyComplete();
    }

    @Test
    @DisplayName("Mono.empty completes without emitting")
    void monoEmptyShouldCompleteWithoutEmitting() {
        StepVerifier.create(Mono.empty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Flux emits multiple elements in order")
    void fluxShouldEmitElementsInOrder() {
        Flux<Integer> flux = Flux.just(1, 2, 3);

        StepVerifier.create(flux)
                .expectNext(1)
                .expectNext(2)
                .expectNext(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("Flux.filter keeps only matching elements")
    void fluxFilterShouldKeepOnlyMatchingElements() {
        Flux<Integer> evens = Flux.range(1, 6).filter(n -> n % 2 == 0);

        StepVerifier.create(evens)
                .expectNext(2)
                .expectNext(4)
                .expectNext(6)
                .verifyComplete();
    }

    @Test
    @DisplayName("Mono.error propagates error to subscriber")
    void monoErrorShouldPropagateError() {
        Mono<String> error = Mono.error(new IllegalArgumentException("id required"));

        StepVerifier.create(error)
                .expectErrorMatches(exception ->
                        exception instanceof IllegalArgumentException &&
                        exception.getMessage().equals("id required"))
                .verify();
    }

    @Test
    @DisplayName("Flux.map transforms each element")
    void fluxMapShouldTransformElements() {
        Flux<String> names = Flux.just("herbicida", "fungicida")
                .map(String::toUpperCase);

        StepVerifier.create(names)
                .expectNext("HERBICIDA")
                .expectNext("FUNGICIDA")
                .verifyComplete();
    }
}
