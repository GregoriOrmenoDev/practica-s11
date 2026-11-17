package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.dto.GeographicLocationDTO;
import pe.edu.vallegrande.farmagro.model.sql.GeographicLocation;
import pe.edu.vallegrande.farmagro.repository.sql.GeographicLocationRepository;
import pe.edu.vallegrande.farmagro.service.GeographicLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GeographicLocationServiceImpl implements GeographicLocationService {

    private final GeographicLocationRepository geographicLocationRepository;

    @Override
    public Flux<GeographicLocationDTO> findAll() {
        return geographicLocationRepository.findAll().map(this::toDTO);
    }

    @Override
    public Mono<GeographicLocationDTO> findById(Integer id) {
        return geographicLocationRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ubigeo no encontrado con id: " + id)))
                .map(this::toDTO);
    }

    @Override
    public Flux<GeographicLocationDTO> findByDepartment(String department) {
        return geographicLocationRepository.findByDepartment(department).map(this::toDTO);
    }

    @Override
    public Flux<GeographicLocationDTO> findByDepartmentAndProvince(String department, String province) {
        return geographicLocationRepository.findByDepartmentAndProvince(department, province).map(this::toDTO);
    }


    private GeographicLocationDTO toDTO(GeographicLocation location) {
        return GeographicLocationDTO.builder()
                .geographicLocationId(location.getGeographicLocationId())
                .department(location.getDepartment())
                .province(location.getProvince())
                .district(location.getDistrict())
                .build();
    }
}
