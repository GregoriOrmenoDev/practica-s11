package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.util.AppConstants;
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import pe.edu.vallegrande.farmagro.service.PersonImportFileParser;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class PersonExcelServiceImpl implements pe.edu.vallegrande.farmagro.service.PersonExcelService {

    private static final String TEMPLATE_RESOURCE = "templates/person-template.csv";
    private static final String WORKBOOK_TITLE = "Persons FarmaGro";
    private static final String MASKED_PASSWORD = "******";
    private static final String MISSING_VALUE_PLACEHOLDER = AppConstants.MISSING_VALUE_PLACEHOLDER;
    private static final String EMPTY_TEXT = AppConstants.EMPTY_TEXT;

    private static final int PERSON_ID_COLUMN = 0;
    private static final int DOCUMENT_TYPE_COLUMN = 1;
    private static final int DOCUMENT_NUMBER_COLUMN = 2;
    private static final int FIRST_NAMES_COLUMN = 3;
    private static final int LAST_NAMES_COLUMN = 4;
    private static final int PHONE_COLUMN = 5;
    private static final int EMAIL_COLUMN = 6;
    private static final int ROLE_COLUMN = 7;
    private static final int ADDRESS_COLUMN = 8;
    private static final int GEOGRAPHIC_LOCATION_COLUMN = 9;
    private static final int INITIAL_PASSWORD_COLUMN = 10;

    private static final String[] HEADERS = {
            "ID (Opcional)",
            "Tipo Documento",
            "Número Documento",
            "Nombres / Razón Social",
            "Apellidos",
            "Teléfono",
            "Email",
            "Rol",
            "Dirección (Clientes)",
            "Ubigeo ID (Clientes)",
            "Contraseña Inicial"
    };

    private final PersonImportFileParser personImportFileParser;

    public Mono<byte[]> exportToExcel(Flux<PersonDTO> persons) {
        return persons.collectList()
                .flatMap(people -> Mono.fromCallable(() -> ExcelExportSupport.createWorkbook(
                        WORKBOOK_TITLE, HEADERS, people, this::fillPersonRow)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void fillPersonRow(Row row, PersonDTO person, CellStyle rowStyle) {
        createCell(row, PERSON_ID_COLUMN, person.getId(), rowStyle);
        createCell(row, DOCUMENT_TYPE_COLUMN, person.getDocumentType(), rowStyle);
        createCell(row, DOCUMENT_NUMBER_COLUMN, person.getDocumentNumber(), rowStyle);
        createCell(row, FIRST_NAMES_COLUMN, person.getFirstNames(), rowStyle);
        createCell(row, LAST_NAMES_COLUMN, person.getLastNames(), rowStyle);
        createCell(row, PHONE_COLUMN, person.getPhone(), rowStyle);
        createCell(row, EMAIL_COLUMN, person.getEmail(), rowStyle);
        createCell(row, ROLE_COLUMN, person.getRole() != null ? person.getRole().toStorageValue() : EMPTY_TEXT, rowStyle);
        createCell(row, ADDRESS_COLUMN, extractCustomerAddress(person), rowStyle);
        createCell(row, GEOGRAPHIC_LOCATION_COLUMN, extractCustomerGeographicLocation(person), rowStyle);
        createCell(row, INITIAL_PASSWORD_COLUMN, MASKED_PASSWORD, rowStyle);
    }

    private String extractCustomerAddress(PersonDTO person) {
        return person.getCustomer() != null ? person.getCustomer().getAddress() : MISSING_VALUE_PLACEHOLDER;
    }

    private String extractCustomerGeographicLocation(PersonDTO person) {
        return person.getCustomer() != null && person.getCustomer().getGeographicLocationId() != null
                ? String.valueOf(person.getCustomer().getGeographicLocationId())
                : MISSING_VALUE_PLACEHOLDER;
    }

    public Mono<byte[]> generateTemplate() {
        return exportToExcel(ImportFileSupport.parseCsvResource(TEMPLATE_RESOURCE, personImportFileParser::parseCsv));
    }

    private void createCell(Row row, int column, String cellText, CellStyle style) {
        ExcelExportSupport.createCell(row, column, cellText, style);
    }
}
