package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import reactor.core.publisher.Flux;

import java.io.InputStream;

public interface PersonImportFileParser {

    Flux<PersonDTO> parseExcel(InputStream input);

    Flux<PersonDTO> parseCsv(InputStream input);
}
