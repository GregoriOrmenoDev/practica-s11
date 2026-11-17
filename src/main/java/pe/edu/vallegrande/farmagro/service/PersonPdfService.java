package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface PersonPdfService {

    Mono<byte[]> exportToPdf(Flux<PersonDTO> persons);
}
