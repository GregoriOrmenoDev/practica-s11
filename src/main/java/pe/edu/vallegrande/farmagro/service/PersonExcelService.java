package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PersonExcelService {

    Mono<byte[]> exportToExcel(Flux<PersonDTO> persons);

    Mono<byte[]> generateTemplate();

}
