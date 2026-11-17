package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.SaleDTO;
import pe.edu.vallegrande.farmagro.dto.SaleDetailDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface SaleService {


    Flux<SaleDTO> findAll();


    Flux<SaleDTO> findAll(int page, int size);


    Flux<SaleDTO> findByStatus(String status, int page, int size);


    Flux<SaleDTO> findByCustomer(String mongoIdCustomer, int page, int size);


    Flux<SaleDTO> findByUser(String mongoIdUser, int page, int size);


    Mono<SaleDTO> findById(Integer id);


    Flux<SaleDetailDTO> findDetailsBySaleId(Integer saleId);


    Mono<SaleDTO> create(SaleDTO dto);


    Mono<SaleDTO> update(Integer id, SaleDTO dto);


    Mono<SaleDTO> cancel(Integer id);


    Mono<SaleDTO> restore(Integer id);


    Mono<SaleDTO> pay(Integer id);
}
