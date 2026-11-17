package pe.edu.vallegrande.farmagro.service.impl;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.farmagro.dto.ImportResultDTO;
import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import pe.edu.vallegrande.farmagro.service.PersonImportFileParser;
import pe.edu.vallegrande.farmagro.service.PersonImportService;
import pe.edu.vallegrande.farmagro.service.PersonService;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonImportServiceImpl implements PersonImportService {

    private static final int COUNT_INCREMENT = 1;

    private final PersonService personService;
    private final PersonImportFileParser personImportFileParser;
    private final Validator validator;

    @Override
    public Mono<ImportResultDTO> importPersons(FilePart filePart) {
        if (filePart == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe seleccionar un archivo para importar"));
        }
        return readPersonFile(filePart)
                .flatMap(this::importPersonRows)
                .onErrorMap(this::asBadRequest);
    }

    private Mono<List<PersonDTO>> readPersonFile(FilePart filePart) {
        return MasterImportFiles.joinContent(filePart).flatMap(buffer -> {
            byte[] fileBytes = MasterImportFiles.readBytesAndRelease(buffer);
            boolean csvFile = MasterImportFiles.isCsv(filePart);
            return Mono.using(() -> new ByteArrayInputStream(fileBytes), input ->
                    (csvFile ? personImportFileParser.parseCsv(input) : personImportFileParser.parseExcel(input))
                            .collectList(), input -> { });
        });
    }

    private Mono<ImportResultDTO> importPersonRows(List<PersonDTO> people) {
        if (people.isEmpty()) {
            return Mono.just(ImportResultDTO.builder()
                    .errors(List.of("El archivo subido no contiene registros válidos para importar"))
                    .build());
        }
        ImportResultDTO result = ImportResultDTO.builder().totalRows(people.size()).build();
        return Flux.fromIterable(people)
                .concatMap(person -> importPersonRow(person, result))
                .then(Mono.just(result));
    }

    private Mono<Void> importPersonRow(PersonDTO person, ImportResultDTO result) {
        return ImportDtoValidation.validate(validator, person)
                .then(personService.create(person))
                .doOnNext(saved -> {
                    result.setImportedCount(result.getImportedCount() + COUNT_INCREMENT);
                    result.getImported().add(saved);
                })
                .then()
                .onErrorResume(error -> {
                    result.setErrorCount(result.getErrorCount() + COUNT_INCREMENT);
                    result.getErrors().add(personImportError(person, error));
                    return Mono.empty();
                });
    }

    private String personImportError(PersonDTO person, Throwable error) {
        return "Doc " + person.getDocumentNumber() + " (" + person.getFirstNames() + "): " + ImportDtoValidation.reason(error);
    }

    private ResponseStatusException asBadRequest(Throwable error) {
        if (error instanceof ResponseStatusException statusException) {
            return statusException;
        }
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Error al procesar el archivo de personas: " + error.getMessage(), error);
    }
}
