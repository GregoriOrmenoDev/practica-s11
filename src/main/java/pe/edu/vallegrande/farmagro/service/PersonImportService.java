package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.ImportResultDTO;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface PersonImportService {

    Mono<ImportResultDTO> importPersons(FilePart filePart);
}
