package pe.edu.vallegrande.farmagro.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.farmagro.repository.PersonRepository;
import pe.edu.vallegrande.farmagro.service.PersonExcelService;
import pe.edu.vallegrande.farmagro.service.PersonExportService;
import pe.edu.vallegrande.farmagro.service.PersonPdfService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PersonExportServiceImpl implements PersonExportService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final PersonExcelService personExcelService;
    private final PersonPdfService personPdfService;

    @Override
    public Mono<byte[]> exportExcel() {
        return personExcelService.exportToExcel(personRepository.findAll().map(personMapper::toDTO))
                .onErrorMap(error -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error al generar el archivo Excel: " + error.getMessage(), error));
    }

    @Override
    public Mono<byte[]> exportPdf() {
        return personPdfService.exportToPdf(personRepository.findAll().map(personMapper::toDTO))
                .onErrorMap(error -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error al generar el archivo PDF: " + error.getMessage(), error));
    }

    @Override
    public Mono<byte[]> getExcelTemplate() {
        return personExcelService.generateTemplate();
    }
}
