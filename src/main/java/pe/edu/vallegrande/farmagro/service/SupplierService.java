package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.SupplierDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface SupplierService {


    Flux<SupplierDTO> findAll(int page, int size);


    Flux<SupplierDTO> findByStatus(Boolean status, int page, int size);


    Mono<SupplierDTO> findById(String id);


    Mono<SupplierDTO> create(SupplierDTO dto);


    Mono<SupplierDTO> update(String id, SupplierDTO dto);


    Mono<SupplierDTO> delete(String id);


    Mono<SupplierDTO> restore(String id);


}
