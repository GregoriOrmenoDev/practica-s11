package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.DeliveryDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface DeliveryService {


    Flux<DeliveryDTO> findAll();


    Mono<DeliveryDTO> findById(Integer id);


    Flux<DeliveryDTO> findByStatus(String status);


    Flux<DeliveryDTO> findBySale(Integer saleId);


    Flux<DeliveryDTO> findByType(String deliveryType);


    Mono<DeliveryDTO> create(DeliveryDTO dto);


    Mono<DeliveryDTO> update(Integer id, DeliveryDTO dto);


    Mono<DeliveryDTO> cancel(Integer id);


    Mono<DeliveryDTO> restore(Integer id);
}
