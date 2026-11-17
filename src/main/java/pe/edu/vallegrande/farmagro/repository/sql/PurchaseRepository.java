package pe.edu.vallegrande.farmagro.repository.sql;

import pe.edu.vallegrande.farmagro.model.sql.Purchase;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PurchaseRepository extends R2dbcRepository<Purchase, Integer> {

    @Query("SELECT * FROM PURCHASES WHERE id_purchase = :id")
    Mono<Purchase> findById(Integer id);

    Flux<Purchase> findByStatus(String status);

    Flux<Purchase> findByMongoIdSupplier(String mongoIdSupplier);

    Flux<Purchase> findByMongoIdUser(String mongoIdUser);

    Mono<Boolean> existsByInvoiceNumber(String invoiceNumber);

    Mono<Boolean> existsByInvoiceNumberAndIdPurchaseNot(String invoiceNumber, Integer idPurchase);

    @Query("SELECT * FROM PURCHASES ORDER BY purchase_date DESC, id_purchase DESC OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY")
    Flux<Purchase> findAllPaged(int offset, int size);

    @Query("SELECT * FROM PURCHASES WHERE status = :status ORDER BY purchase_date DESC, id_purchase DESC OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY")
    Flux<Purchase> findByStatusPaged(String status, int offset, int size);

    @Query("SELECT COUNT(*) FROM PURCHASES")
    Mono<Long> countAll();
}
