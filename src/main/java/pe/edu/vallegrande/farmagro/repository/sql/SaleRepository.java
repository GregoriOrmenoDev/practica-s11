package pe.edu.vallegrande.farmagro.repository.sql;

import pe.edu.vallegrande.farmagro.model.sql.Sale;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SaleRepository extends R2dbcRepository<Sale, Integer> {

    @Query("SELECT * FROM SALES WHERE id_sale = :id")
    Mono<Sale> findById(Integer id);

    @Query("SELECT * FROM SALES WHERE status = :status ORDER BY sale_date DESC")
    Flux<Sale> findByStatus(String status);

    Flux<Sale> findByMongoIdCustomer(String mongoIdCustomer);

    Flux<Sale> findByMongoIdUser(String mongoIdUser);

    Flux<Sale> findBySalesChannel(String salesChannel);

    Mono<Boolean> existsByInvoiceNumber(String invoiceNumber);

    Mono<Boolean> existsByInvoiceNumberAndIdSaleNot(String invoiceNumber, Integer idSale);

    Mono<Boolean> existsByMongoIdCustomer(String mongoIdCustomer);

    Mono<Boolean> existsByMongoIdUser(String mongoIdUser);

    @Query("SELECT * FROM SALES ORDER BY sale_date DESC, id_sale DESC OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY")
    Flux<Sale> findAllPaged(int offset, int size);

    @Query("SELECT * FROM SALES WHERE status = :status ORDER BY sale_date DESC, id_sale DESC OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY")
    Flux<Sale> findByStatusPaged(String status, int offset, int size);

    @Query("SELECT * FROM SALES WHERE mongo_id_customer = :customerId ORDER BY sale_date DESC, id_sale DESC OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY")
    Flux<Sale> findByCustomerPaged(String customerId, int offset, int size);

    @Query("SELECT * FROM SALES WHERE mongo_id_user = :userId ORDER BY sale_date DESC, id_sale DESC OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY")
    Flux<Sale> findByUserPaged(String userId, int offset, int size);

    // Highest correlative of a voucher series (e.g. 'B001-%'); 0 when the series has no sales yet.
    // UPDLOCK + HOLDLOCK keep the series range locked until commit: concurrent sales of the same
    // series wait here instead of reading the same number or deadlocking on the insert.
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(invoice_number, CHARINDEX('-', invoice_number) + 1, 8) AS INT)), 0) "
            + "FROM SALES WITH (UPDLOCK, HOLDLOCK) WHERE invoice_number LIKE :seriesPattern")
    Mono<Integer> findMaxCorrelative(String seriesPattern);

    @Query("SELECT COUNT(*) FROM SALES")
    Mono<Long> countAll();

    @Query("SELECT COUNT(*) FROM SALES WHERE status = :status")
    Mono<Long> countByStatus(String status);
}
