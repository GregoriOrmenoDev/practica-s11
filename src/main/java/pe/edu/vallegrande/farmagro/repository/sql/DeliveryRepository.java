package pe.edu.vallegrande.farmagro.repository.sql;

import pe.edu.vallegrande.farmagro.model.sql.Delivery;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DeliveryRepository extends R2dbcRepository<Delivery, Integer> {

    @Query("SELECT * FROM DELIVERIES WHERE id_delivery = :id")
    Mono<Delivery> findById(Integer id);

    Flux<Delivery> findByStatus(String status);

    Flux<Delivery> findBySaleId(Integer saleId);

    Flux<Delivery> findByGeographicLocationId(Integer geographicLocationId);

    Flux<Delivery> findByDeliveryType(String deliveryType);
}
