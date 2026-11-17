package pe.edu.vallegrande.farmagro.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.vallegrande.farmagro.dto.NewBatchDTO;
import pe.edu.vallegrande.farmagro.dto.PurchaseDetailDTO;
import pe.edu.vallegrande.farmagro.model.Product;
import pe.edu.vallegrande.farmagro.repository.ProductRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Creates the batches that purchase lines bring. A new batch starts with zero stock: its units only
 * arrive when the reception is confirmed (PAID), like any other purchase.
 */
@Slf4j
@Service
@RequiredArgsConstructor
final class PurchaseBatchService {

    record CreatedBatch(String productId, String presentationId, String batchId) {
    }

    private final ProductRepository productRepository;
    private final ProductEntityFactory productEntityFactory;

    /** Creates the new batch of every line that brings one and fills its mongoIdBatch. */
    Mono<List<CreatedBatch>> createNewBatches(List<PurchaseDetailDTO> details) {
        return Flux.fromIterable(details)
                .filter(detail -> detail.getNewBatch() != null)
                .concatMap(this::createBatch)
                .collectList();
    }

    /** Compensation when the purchase fails after its batches were created: they are still empty. */
    Mono<Void> removeBatches(List<CreatedBatch> createdBatches) {
        return Flux.fromIterable(createdBatches)
                .concatMap(created -> Mono.defer(() -> productRepository.findById(created.productId())
                                .flatMap(product -> removeEmptyBatch(product, created)))
                        .retryWhen(OptimisticRetry.onVersionConflict())
                        .onErrorResume(error -> {
                            log.error("Could not remove batch {} after a failed purchase", created.batchId(), error);
                            return Mono.empty();
                        }))
                .then();
    }

    private Mono<CreatedBatch> createBatch(PurchaseDetailDTO detail) {
        return Mono.defer(() -> productRepository.findById(detail.getMongoIdProduct())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "El producto con id " + detail.getMongoIdProduct() + " no existe en el catálogo")))
                        .flatMap(product -> addBatch(product, detail)))
                .retryWhen(OptimisticRetry.onVersionConflict())
                .doOnNext(created -> detail.setMongoIdBatch(created.batchId()));
    }

    private Mono<CreatedBatch> addBatch(Product product, PurchaseDetailDTO detail) {
        Map<String, Object> presentation = CatalogInventorySupport.findPresentation(product, detail.getMongoIdPresentation());
        if (presentation == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La presentación " + detail.getMongoIdPresentation() + " no existe en el producto " + product.getName()));
        }
        String batchId = productEntityFactory.newBatchId(product);
        List<Object> batches = new ArrayList<>(presentation.get(CatalogFields.BATCHES) instanceof List<?> stored ? stored : List.of());
        batches.add(toBatch(batchId, detail.getNewBatch()));
        presentation.put(CatalogFields.BATCHES, batches);
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product)
                .thenReturn(new CreatedBatch(product.getId(), detail.getMongoIdPresentation(), batchId));
    }

    private Mono<Void> removeEmptyBatch(Product product, CreatedBatch created) {
        Map<String, Object> presentation = CatalogInventorySupport.findPresentation(product, created.presentationId());
        if (presentation == null || !(presentation.get(CatalogFields.BATCHES) instanceof List<?> stored)) return Mono.empty();
        List<Object> remaining = new ArrayList<>(stored);
        boolean removed = remaining.removeIf(batch -> batch instanceof Map<?, ?> entry
                && Objects.equals(CatalogValueReader.entryId(entry), created.batchId())
                && CatalogInventorySupport.batchStock(CatalogInventorySupport.asStringMap(entry)) == 0);
        if (!removed) return Mono.empty();
        presentation.put(CatalogFields.BATCHES, remaining);
        return productRepository.save(product).then();
    }

    private Map<String, Object> toBatch(String batchId, NewBatchDTO newBatch) {
        Map<String, Object> batch = new HashMap<>();
        batch.put(CatalogFields.ID, batchId);
        batch.put(CatalogFields.BATCH_NUMBER, newBatch.getBatchNumber().trim());
        if (newBatch.getManufactureDate() != null) {
            batch.put(CatalogFields.MANUFACTURE_DATE, Date.from(newBatch.getManufactureDate().atStartOfDay(ZoneOffset.UTC).toInstant()));
        }
        batch.put(CatalogFields.EXPIRATION_DATE, Date.from(newBatch.getExpirationDate().atStartOfDay(ZoneOffset.UTC).toInstant()));
        batch.put(CatalogFields.BATCH_STOCK, 0);
        batch.put(CatalogFields.STATUS, true);
        batch.put(CatalogFields.CREATED_AT, new Date());
        return batch;
    }
}
