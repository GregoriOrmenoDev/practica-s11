package pe.edu.vallegrande.farmagro.rest;

import pe.edu.vallegrande.farmagro.dto.GeographicLocationDTO;
import pe.edu.vallegrande.farmagro.service.GeographicLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/ubigeo")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GeographicLocationRest {

    private final GeographicLocationService geographicLocationService;

    @GetMapping
    public Flux<GeographicLocationDTO> getAll() {
        return geographicLocationService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<GeographicLocationDTO> getById(@PathVariable Integer id) {
        return geographicLocationService.findById(id);
    }

    @GetMapping("/department/{department}")
    public Flux<GeographicLocationDTO> getByDepartment(@PathVariable String department) {
        return geographicLocationService.findByDepartment(department);
    }

    @GetMapping("/department/{department}/province/{province}")
    public Flux<GeographicLocationDTO> getByDepartmentAndProvince(@PathVariable String department,
                                                        @PathVariable String province) {
        return geographicLocationService.findByDepartmentAndProvince(department, province);
    }
}
