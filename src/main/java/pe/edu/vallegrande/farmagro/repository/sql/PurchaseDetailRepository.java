package pe.edu.vallegrande.farmagro.repository.sql;

import pe.edu.vallegrande.farmagro.model.sql.PurchaseDetail;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface PurchaseDetailRepository extends R2dbcRepository<PurchaseDetail, Integer> {

    @Query("SELECT * FROM PURCHASE_DETAILS WHERE purchases_id_purchase = :purchasesIdPurchase")
    Flux<PurchaseDetail> findByPurchasesIdPurchase(Integer purchasesIdPurchase);

    Flux<PurchaseDetail> findByMongoIdProduct(String mongoIdProduct);

    Flux<PurchaseDetail> findByMongoIdPresentation(String mongoIdPresentation);
}
