package pe.edu.vallegrande.farmagro.repository.sql;

import pe.edu.vallegrande.farmagro.model.sql.GeographicLocation;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface GeographicLocationRepository extends R2dbcRepository<GeographicLocation, Integer> {

    Flux<GeographicLocation> findByDepartment(String department);

    Flux<GeographicLocation> findByDepartmentAndProvince(String department, String province);
}
