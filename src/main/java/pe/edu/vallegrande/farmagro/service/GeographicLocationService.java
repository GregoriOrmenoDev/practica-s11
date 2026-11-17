package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.GeographicLocationDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface GeographicLocationService {


    Flux<GeographicLocationDTO> findAll();


    Mono<GeographicLocationDTO> findById(Integer id);


    Flux<GeographicLocationDTO> findByDepartment(String department);


    Flux<GeographicLocationDTO> findByDepartmentAndProvince(String department, String province);
}
