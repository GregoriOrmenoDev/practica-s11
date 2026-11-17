package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.util.TextNormalizer;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.dto.CustomerDTO;
import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import pe.edu.vallegrande.farmagro.model.PersonRole;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.InputStream;

@Slf4j
@Service
public class PersonImportFileParserImpl implements pe.edu.vallegrande.farmagro.service.PersonImportFileParser {

    private record PersonImportRow(
            PersonIdentity identity,
            PersonContact contact,
            PersonAccess access,
            CustomerDTO customer) {
    }

    private record PersonAccess(PersonRole role, String password) {
    }

    private record PersonNames(String firstNames, String lastNames) {
    }

    private record PersonIdentity(
            String id,
            String documentType,
            String documentNumber,
            PersonNames names) {
    }

    private record PersonContact(String phone, String email) {
    }

    // UBIGEO ids are positive, so 0 never matches a row and the import reports that row as invalid.
    private static final int INVALID_GEOGRAPHIC_LOCATION_ID = 0;
    private static final String DEFAULT_ROLE = PersonRole.CUSTOMER.toStorageValue();
    private static final String DEFAULT_PASSWORD = "Farmagro2026*";
    private static final String DEFAULT_DOCUMENT_TYPE = AppConstants.DOCUMENT_TYPE_DNI;
    private static final String DEFAULT_PHONE_NUMBER = "900000000";
    private static final String DEFAULT_CUSTOMER_ADDRESS = "Dirección central";
    private static final String MASKED_PASSWORD = "******";

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
    private static final int PERSON_COLUMN_COUNT = INITIAL_PASSWORD_COLUMN + 1;
    private static final int FIRST_DATA_ROW_INDEX = 1;
    private static final int FIRST_WORKSHEET_INDEX = 0;
    private static final int MINIMUM_CSV_FIELDS = 4;

    public Flux<PersonDTO> parseExcel(InputStream is) {
        return ImportFileSupport.parseExcel(is, FIRST_WORKSHEET_INDEX, FIRST_DATA_ROW_INDEX,
                row -> isEmptyRow(row) ? null : buildPersonDtoFromRow(row));
    }

    private boolean isEmptyRow(Row row) {
        String documentType = ImportFileSupport.getIntegerCellValue(row.getCell(DOCUMENT_TYPE_COLUMN));
        String documentNumber = ImportFileSupport.getIntegerCellValue(row.getCell(DOCUMENT_NUMBER_COLUMN));
        return TextNormalizer.isBlank(documentType) && TextNormalizer.isBlank(documentNumber);
    }

    private PersonDTO buildPersonDtoFromRow(Row row) {
        String[] rowValues = new String[PERSON_COLUMN_COUNT];
        for (int columnIndex = 0; columnIndex < PERSON_COLUMN_COUNT; columnIndex++) {
            rowValues[columnIndex] = ImportFileSupport.getIntegerCellValue(row.getCell(columnIndex));
        }
        return buildPersonDtoFromValues(rowValues, true);
    }

    private PersonDTO buildPersonDtoFromValues(String[] personCells, boolean excelFormat) {
        return toPersonDTO(buildImportRow(personCells, excelFormat));
    }

    private PersonImportRow buildImportRow(String[] personCells, boolean excelFormat) {
        PersonRole role = readImportRole(personCells);
        return new PersonImportRow(
                buildPersonIdentity(personCells, excelFormat),
                buildPersonContact(personCells, excelFormat),
                new PersonAccess(role, resolveImportPassword(personCells, excelFormat)),
                buildImportCustomer(personCells, role));
    }

    private PersonRole readImportRole(String[] personCells) {
        return normalizeRoleName(cellAt(personCells, ROLE_COLUMN, DEFAULT_ROLE));
    }

    private PersonIdentity buildPersonIdentity(String[] personCells, boolean excelFormat) {
        String documentType = cellAt(personCells, DOCUMENT_TYPE_COLUMN, DEFAULT_DOCUMENT_TYPE);
        String documentNumber = cellAt(personCells, DOCUMENT_NUMBER_COLUMN, AppConstants.EMPTY_TEXT);
        return new PersonIdentity(
                normalizeId(cellAt(personCells, PERSON_ID_COLUMN, null)),
                normalizeDocumentType(documentType),
                normalizeImportedDocumentNumber(documentNumber, excelFormat),
                new PersonNames(
                        trimOrEmpty(cellAt(personCells, FIRST_NAMES_COLUMN, AppConstants.EMPTY_TEXT)),
                        getLastNames(cellAt(personCells, LAST_NAMES_COLUMN, AppConstants.EMPTY_TEXT), documentType)));
    }

    private String normalizeImportedDocumentNumber(String documentNumber, boolean excelFormat) {
        return excelFormat ? normalizeExcelDocumentNumber(documentNumber) : trimOrEmpty(documentNumber);
    }

    private PersonContact buildPersonContact(String[] personCells, boolean excelFormat) {
        String phone = cellAt(personCells, PHONE_COLUMN, excelFormat ? DEFAULT_PHONE_NUMBER : AppConstants.EMPTY_TEXT);
        String normalizedPhone = excelFormat ? TextNormalizer.stripExcelDecimal(phone) : trimOrEmpty(phone);
        return new PersonContact(normalizedPhone,
                normalizeEmail(cellAt(personCells, EMAIL_COLUMN, AppConstants.EMPTY_TEXT)));
    }

    private String resolveImportPassword(String[] personCells, boolean excelFormat) {
        String password = cellAt(personCells, INITIAL_PASSWORD_COLUMN, DEFAULT_PASSWORD);
        return excelFormat ? getPasswordHash(password) : normalizePassword(password);
    }

    private CustomerDTO buildImportCustomer(String[] personCells, PersonRole role) {
        String address = cellAt(personCells, ADDRESS_COLUMN, AppConstants.EMPTY_TEXT);
        String locationId = cellAt(personCells, GEOGRAPHIC_LOCATION_COLUMN, AppConstants.EMPTY_TEXT);
        return buildCustomerForRole(role, address, locationId);
    }

    private String cellAt(String[] personCells, int columnIndex, String defaultText) {
        return columnIndex < personCells.length && personCells[columnIndex] != null
                ? personCells[columnIndex] : defaultText;
    }

    private String normalizePassword(String password) {
        return password != null && !password.isBlank() ? password.trim() : DEFAULT_PASSWORD;
    }

    private PersonDTO toPersonDTO(PersonImportRow row) {
        return PersonDTO.builder()
                .id(row.identity().id())
                .documentType(row.identity().documentType())
                .documentNumber(row.identity().documentNumber())
                .firstNames(row.identity().names().firstNames())
                .lastNames(row.identity().names().lastNames())
                .phone(row.contact().phone())
                .email(row.contact().email())
                .role(row.access().role())
                .passwordHash(row.access().password())
                .customer(row.customer())
                .status(true)
                .build();
    }

    private String normalizeId(String id) {
        return id == null || id.isBlank() ? null : id.trim();
    }

    private String normalizeDocumentType(String documentType) {
        return documentType == null ? DEFAULT_DOCUMENT_TYPE : TextNormalizer.code(documentType);
    }

    private String normalizeExcelDocumentNumber(String documentNumber) {
        return documentNumber == null ? AppConstants.EMPTY_TEXT : TextNormalizer.stripExcelDecimal(documentNumber);
    }

    private String trimOrEmpty(String text) {
        return TextNormalizer.trimOrEmpty(text);
    }

    private String normalizeEmail(String email) {
        return email == null ? AppConstants.EMPTY_TEXT : TextNormalizer.email(email);
    }

    private PersonRole normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return PersonRole.CUSTOMER;
        }
        try {
            return PersonRole.fromValue(roleName);
        } catch (IllegalArgumentException illegalArgumentException) {
            log.warn("Invalid person role in import file: {}", roleName);
            return null;
        }
    }

    private CustomerDTO buildCustomerForRole(PersonRole role, String address, String geographicLocationValue) {
        if (PersonRole.CUSTOMER != role) {
            return null;
        }

        Integer geographicLocationId = parseGeographicLocationId(geographicLocationValue);
        String addressValue = isMissing(address) ? DEFAULT_CUSTOMER_ADDRESS : address.trim();

        return CustomerDTO.builder()
                .address(addressValue)
                .geographicLocationId(geographicLocationId)
                .build();
    }

    private Integer parseGeographicLocationId(String geographicLocationValue) {
        if (isMissing(geographicLocationValue)) {
            return null; // no location: it is chosen when a delivery is scheduled
        }
        try {
            return Integer.parseInt(TextNormalizer.stripExcelDecimal(geographicLocationValue));
        } catch (NumberFormatException numberFormatException) {
            log.warn("Invalid geographicLocation format in import file: {}", geographicLocationValue);
        }
        return INVALID_GEOGRAPHIC_LOCATION_ID;
    }

    private String getLastNames(String lastNames, String documentType) {
        if (lastNames != null && !lastNames.isBlank()) {
            return lastNames.trim();
        }
        return AppConstants.DOCUMENT_TYPE_RUC.equalsIgnoreCase(documentType)
                ? AppConstants.MISSING_VALUE_PLACEHOLDER : AppConstants.EMPTY_TEXT;
    }

    private boolean isMissing(String cellText) {
        return TextNormalizer.isBlank(cellText) || AppConstants.MISSING_VALUE_PLACEHOLDER.equals(cellText.trim());
    }

    private String getPasswordHash(String password) {
        if (password != null && !password.isBlank() && !MASKED_PASSWORD.equals(password)) {
            return password.trim();
        }
        return DEFAULT_PASSWORD;
    }

    public Flux<PersonDTO> parseCsv(InputStream is) {
        return ImportFileSupport.parseCsv(is, FIRST_DATA_ROW_INDEX, MINIMUM_CSV_FIELDS, this::buildPersonDto);
    }

    private PersonDTO buildPersonDto(String[] csvFields) {
        return buildPersonDtoFromValues(csvFields, false);
    }
}
