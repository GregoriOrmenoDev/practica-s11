package pe.edu.vallegrande.farmagro.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import pe.edu.vallegrande.farmagro.dto.SaleDetailDTO;
import pe.edu.vallegrande.farmagro.model.Product;
import pe.edu.vallegrande.farmagro.repository.ProductRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockConcurrencyTest {

    @Test
    @DisplayName("A stale stock write is retried on a fresh copy instead of overwriting the concurrent change")
    void saleStockShouldRetryOnVersionConflict() {
        ProductRepository repository = mock(ProductRepository.class);
        AtomicInteger reads = new AtomicInteger();
        // The second read sees the 3 units another sale took meanwhile (stock 10 -> 7).
        when(repository.findById("PRD900")).thenAnswer(call ->
                Mono.fromSupplier(() -> product(reads.getAndIncrement() == 0 ? 10 : 7)));
        AtomicInteger saves = new AtomicInteger();
        Map<String, Object> savedPresentation = new HashMap<>();
        when(repository.save(any(Product.class))).thenAnswer(call -> {
            if (saves.getAndIncrement() == 0) {
                return Mono.error(new OptimisticLockingFailureException("version changed"));
            }
            Product saved = call.getArgument(0);
            savedPresentation.putAll(CatalogInventorySupport.findPresentation(saved, "PST900"));
            return Mono.just(saved);
        });
        SaleDetailDTO line = new SaleDetailDTO();
        line.setMongoIdProduct("PRD900");
        line.setMongoIdPresentation("PST900");
        line.setMongoIdBatch("LOT900");
        line.setQuantity(2);

        StepVerifier.create(new SaleInventoryService(repository).adjustWithRollback(List.of(line), -1)).verifyComplete();

        verify(repository, times(2)).findById("PRD900");
        assertEquals(5, savedPresentation.get(CatalogFields.CURRENT_STOCK));
    }

    private Product product(int stock) {
        Map<String, Object> batch = new HashMap<>(Map.of(CatalogFields.ID, "LOT900", CatalogFields.BATCH_STOCK, stock));
        Map<String, Object> presentation = new HashMap<>(Map.of(CatalogFields.ID, "PST900",
                CatalogFields.CURRENT_STOCK, stock, CatalogFields.BATCHES, List.of(batch)));
        return Product.builder().id("PRD900").name("Test").version(1L).presentations(List.of(presentation)).build();
    }
}
