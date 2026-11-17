package pe.edu.vallegrande.farmagro.repository.sql;

import pe.edu.vallegrande.farmagro.model.sql.SaleDetail;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SaleDetailRepository extends R2dbcRepository<SaleDetail, Integer> {

    @Query("SELECT * FROM SALE_DETAILS WHERE sales_id_sale = :saleId")
    Flux<SaleDetail> findBySaleId(Integer saleId);

    Flux<SaleDetail> findByMongoIdPresentation(String mongoIdPresentation);

    Flux<SaleDetail> findByMongoIdBatch(String mongoIdBatch);

    @Modifying
    @Query("DELETE FROM SALE_DETAILS WHERE sales_id_sale = :saleId")
    Mono<Void> deleteAllBySaleId(Integer saleId);
}
