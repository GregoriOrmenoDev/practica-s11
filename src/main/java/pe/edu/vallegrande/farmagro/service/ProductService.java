package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.ProductDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductService {


    Flux<ProductDTO> findAll(int page, int size);


    Flux<ProductDTO> findAllActive(int page, int size);


    Flux<ProductDTO> findAllDeleted(int page, int size);


    Flux<ProductDTO> findByCategory(String category);


    Mono<ProductDTO> findById(String id);


    Mono<ProductDTO> create(ProductDTO dto);


    Mono<ProductDTO> update(String id, ProductDTO dto);


    Mono<Void> delete(String id);


    Mono<ProductDTO> restore(String id);


}
