package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.PurchaseDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface PurchaseService {


    Flux<PurchaseDTO> findAll(int page, int size);


    Flux<PurchaseDTO> findByStatus(String status, int page, int size);


    Flux<PurchaseDTO> findBySupplier(String mongoIdSupplier);


    Mono<PurchaseDTO> findById(Integer id);


    Mono<PurchaseDTO> create(PurchaseDTO dto);


    Mono<PurchaseDTO> update(Integer id, PurchaseDTO dto);


    Mono<PurchaseDTO> delete(Integer id);


    Mono<PurchaseDTO> restore(Integer id);
}
