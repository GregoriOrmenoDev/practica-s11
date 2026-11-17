package pe.edu.vallegrande.farmagro.service.impl;

import org.springframework.http.HttpStatus;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

final class DetailSignatureSupport {

    private static final String SEPARATOR = "|";

    record CatalogItemRef(String productId, String presentationId, String batchId) {
    }

    record DetailSignature(CatalogItemRef catalogItem, Integer quantity, BigDecimal unitPrice, BigDecimal amount) {
    }

    private DetailSignatureSupport() {
    }

    static String compose(DetailSignature signature) {
        return String.join(SEPARATOR,
                String.valueOf(signature.catalogItem().productId()),
                String.valueOf(signature.catalogItem().presentationId()),
                String.valueOf(signature.catalogItem().batchId()),
                String.valueOf(signature.quantity()),
                decimalKey(signature.unitPrice()),
                decimalKey(signature.amount()));
    }

    static <T> Mono<Void> validateUnchanged(Flux<T> existingDetails, List<String> requestedSignatures,
                                            Function<T, String> signatureExtractor, String errorMessage) {
        return existingDetails
                .map(signatureExtractor)
                .collectList()
                .flatMap(existingSignatures -> signaturesMatch(existingSignatures, requestedSignatures)
                        ? Mono.empty()
                        : Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage)));
    }

    static String decimalKey(BigDecimal amount) {
        return amount == null ? AppConstants.EMPTY_TEXT : amount.stripTrailingZeros().toPlainString();
    }

    private static boolean signaturesMatch(List<String> existingSignatures, List<String> requestedSignatures) {
        return existingSignatures.stream().sorted().toList()
                .equals(requestedSignatures.stream().sorted().toList());
    }
}
