package pe.edu.vallegrande.farmagro.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.vallegrande.farmagro.dto.SaleDetailDTO;
import pe.edu.vallegrande.farmagro.model.sql.Sale;
import pe.edu.vallegrande.farmagro.repository.sql.SaleRepository;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
final class SaleTransitionService {

    private static final int STOCK_DECREASE = -1;
    private static final int STOCK_INCREASE = 1;

    private final SaleRepository saleRepository;
    private final SaleInventoryService saleInventoryService;

    private record InventoryTransition(
            int inventoryDirection,
            int rollbackDirection,
            Consumer<Sale> apply,
            String rollbackFailureMessage) {
    }

    Mono<Sale> cancel(Sale sale, List<SaleDetailDTO> details) {
        return applyWithCompensation(sale, details, new InventoryTransition(
                STOCK_INCREASE, STOCK_DECREASE, this::markCancelled,
                "Failed to restore sale inventory after SQL update failure"));
    }

    Mono<Sale> restore(Sale sale, List<SaleDetailDTO> details) {
        return applyWithCompensation(sale, details, new InventoryTransition(
                STOCK_DECREASE, STOCK_INCREASE, this::markRestored,
                "Failed to reverse restored inventory after SQL update failure"));
    }

    Mono<Void> adjustStockForNewSale(List<SaleDetailDTO> details) {
        return saleInventoryService.adjustWithRollback(details, STOCK_DECREASE);
    }

    // No shared Mongo/SQL transaction: stock is reverted if SQL fails.
    private Mono<Sale> applyWithCompensation(
            Sale sale, List<SaleDetailDTO> details, InventoryTransition transition) {
        return saleInventoryService.adjustWithRollback(details, transition.inventoryDirection())
                .then(Mono.defer(() -> saveAfterTransition(sale, details, transition)));
    }

    private Mono<Sale> saveAfterTransition(
            Sale sale, List<SaleDetailDTO> details, InventoryTransition transition) {
        transition.apply().accept(sale);
        return saleRepository.save(sale)
                .onErrorResume(error -> saleInventoryService.adjustWithRollback(
                                details, transition.rollbackDirection())
                        .onErrorResume(rollbackError -> {
                            log.error(transition.rollbackFailureMessage(), rollbackError);
                            return Mono.empty();
                        })
                        .then(Mono.error(error)));
    }

    private void markCancelled(Sale sale) {
        LocalDateTime now = LocalDateTime.now();
        sale.setStatus(AppConstants.STATUS_CANCELLED);
        sale.setDeletedAt(now);
        sale.setRestoredAt(null);
        sale.setUpdatedAt(now);
    }

    private void markRestored(Sale sale) {
        LocalDateTime now = LocalDateTime.now();
        sale.setStatus(AppConstants.STATUS_PENDING);
        sale.setRestoredAt(now);
        sale.setDeletedAt(null);
        sale.setUpdatedAt(now);
    }
}
