package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.util.TextNormalizer;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.dto.SupplierDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SupplierExcelServiceImpl implements pe.edu.vallegrande.farmagro.service.SupplierExcelService {

    private static final String TEMPLATE_RESOURCE = "templates/supplier-template.csv";
    private static final String WORKBOOK_TITLE = "Proveedores FarmaGro";
    private static final String DEFAULT_DOCUMENT_TYPE = AppConstants.DOCUMENT_TYPE_RUC;
    // UBIGEO ids are positive, so 0 never matches a row and the import reports that row as invalid.
    private static final int INVALID_GEOGRAPHIC_LOCATION_ID = 0;
    private static final String DEFAULT_ISO_CERTIFICATION = "false";
    private static final String EMPTY_TEXT = AppConstants.EMPTY_TEXT;
    private static final String MISSING_VALUE_PLACEHOLDER = AppConstants.MISSING_VALUE_PLACEHOLDER;
    private static final String ISO_CERTIFICATION_KEY = "iso9001";
    private static final String SENASA_REGISTRATION_KEY = "senasa_reg";
    private static final String EXPORT_CATEGORY_DELIMITER = ", ";
    private static final String EXCEL_CATEGORY_DELIMITER_PATTERN = ",";
    private static final String CSV_CATEGORY_DELIMITER_PATTERN = "[|,]";
    private static final int FIRST_WORKSHEET_INDEX = 0;
    private static final int FIRST_DATA_ROW_INDEX = 1;
    private static final int MINIMUM_CSV_FIELDS = 4;

    private static final int SUPPLIER_ID_COLUMN = 0;
    private static final int DOCUMENT_TYPE_COLUMN = 1;
    private static final int DOCUMENT_NUMBER_COLUMN = 2;
    private static final int BUSINESS_NAME_COLUMN = 3;
    private static final int CONTACT_NAME_COLUMN = 4;
    private static final int PHONE_COLUMN = 5;
    private static final int EMAIL_COLUMN = 6;
    private static final int ADDRESS_COLUMN = 7;
    private static final int GEOGRAPHIC_LOCATION_COLUMN = 8;
    private static final int PRODUCT_CATEGORIES_COLUMN = 9;
    private static final int ISO_CERTIFICATION_COLUMN = 10;
    private static final int SENASA_REGISTRATION_COLUMN = 11;

    private static final String[] HEADERS = {
            "ID (Opcional)",
            "Tipo Documento",
            "Número Documento",
            "Razón Social",
            "Contacto",
            "Teléfono",
            "Email",
            "Dirección",
            "Ubigeo ID",
            "Product Categories",
            "ISO9001 (true/false)",
            "SENASA Reg"
    };

    @Override
    public Mono<byte[]> exportToExcel(List<SupplierDTO> suppliers) {
        return Mono.fromCallable(() -> ExcelExportSupport.createWorkbook(
                        WORKBOOK_TITLE, HEADERS, suppliers, this::writeSupplierRow))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void writeSupplierRow(Row row, SupplierDTO supplier, CellStyle style) {
        createCell(row, SUPPLIER_ID_COLUMN, supplier.getId(), style);
        createCell(row, DOCUMENT_TYPE_COLUMN, supplier.getDocumentType(), style);
        createCell(row, DOCUMENT_NUMBER_COLUMN, supplier.getDocumentNumber(), style);
        createCell(row, BUSINESS_NAME_COLUMN, supplier.getBusinessName(), style);
        createCell(row, CONTACT_NAME_COLUMN, supplier.getContactName(), style);
        createCell(row, PHONE_COLUMN, supplier.getPhone(), style);
        createCell(row, EMAIL_COLUMN, supplier.getEmail(), style);
        createCell(row, ADDRESS_COLUMN, supplier.getAddress(), style);
        createCell(row, GEOGRAPHIC_LOCATION_COLUMN, locationIdText(supplier.getGeographicLocationId()), style);
        createCell(row, PRODUCT_CATEGORIES_COLUMN, joinCategories(supplier.getProductCategories()), style);
        writeCertificationCells(row, supplier.getCertifications(), style);
    }

    private void writeCertificationCells(Row row, Map<String, Object> certifications, CellStyle style) {
        createCell(row, ISO_CERTIFICATION_COLUMN, certificationText(certifications, ISO_CERTIFICATION_KEY), style);
        createCell(row, SENASA_REGISTRATION_COLUMN,
                certificationText(certifications, SENASA_REGISTRATION_KEY), style);
    }

    private String certificationText(Map<String, Object> certifications, String certificationKey) {
        if (certifications == null || certifications.get(certificationKey) == null) {
            return MISSING_VALUE_PLACEHOLDER;
        }
        return String.valueOf(certifications.get(certificationKey));
    }

    private String locationIdText(Integer geographicLocationId) {
        return geographicLocationId == null ? MISSING_VALUE_PLACEHOLDER : String.valueOf(geographicLocationId);
    }

    private String joinCategories(List<String> categories) {
        return categories == null ? EMPTY_TEXT : String.join(EXPORT_CATEGORY_DELIMITER, categories);
    }

    @Override
    public Mono<byte[]> generateTemplate() {
        return ImportFileSupport.parseCsvResource(TEMPLATE_RESOURCE, this::parseCsv)
                .collectList()
                .flatMap(this::exportToExcel);
    }

    @Override
    public Flux<SupplierDTO> parseExcel(InputStream input) {
        return ImportFileSupport.parseExcel(input, FIRST_WORKSHEET_INDEX, FIRST_DATA_ROW_INDEX,
                row -> parseSupplierRow(readExcelCells(row), EXCEL_CATEGORY_DELIMITER_PATTERN, true));
    }

    private String[] readExcelCells(Row row) {
        String[] supplierCells = new String[HEADERS.length];
        for (int columnIndex = 0; columnIndex < supplierCells.length; columnIndex++) {
            supplierCells[columnIndex] = ImportFileSupport.getIntegerCellValue(row.getCell(columnIndex));
        }
        return supplierCells;
    }

    @Override
    public Flux<SupplierDTO> parseCsv(InputStream input) {
        return ImportFileSupport.parseCsv(input, FIRST_DATA_ROW_INDEX, MINIMUM_CSV_FIELDS, this::parseCsvRow);
    }

    private SupplierDTO parseCsvRow(String[] csvFields) {
        return parseSupplierRow(trimFields(csvFields), CSV_CATEGORY_DELIMITER_PATTERN, false);
    }

    private String[] trimFields(String[] csvFields) {
        String[] trimmedFields = new String[csvFields.length];
        for (int columnIndex = 0; columnIndex < csvFields.length; columnIndex++) {
            trimmedFields[columnIndex] = trimOrEmpty(csvFields[columnIndex]);
        }
        return trimmedFields;
    }

    private SupplierDTO parseSupplierRow(String[] supplierCells, String categoryPattern, boolean excelFormat) {
        String documentType = cellAt(supplierCells, DOCUMENT_TYPE_COLUMN, DEFAULT_DOCUMENT_TYPE);
        String documentNumber = cellAt(supplierCells, DOCUMENT_NUMBER_COLUMN, EMPTY_TEXT);
        if (documentType.isBlank() && documentNumber.isBlank()) {
            return null;
        }
        return buildSupplierDto(supplierCells, categoryPattern, excelFormat);
    }

    private SupplierDTO buildSupplierDto(String[] supplierCells, String categoryPattern, boolean excelFormat) {
        SupplierDTO supplier = SupplierDTO.builder().status(true).build();
        applyIdentity(supplier, supplierCells);
        applyContact(supplier, supplierCells, excelFormat);
        applyClassification(supplier, supplierCells, categoryPattern);
        return supplier;
    }

    private void applyIdentity(SupplierDTO supplier, String[] supplierCells) {
        supplier.setId(blankToNull(cellAt(supplierCells, SUPPLIER_ID_COLUMN, EMPTY_TEXT)));
        supplier.setDocumentType(TextNormalizer.code(cellAt(supplierCells, DOCUMENT_TYPE_COLUMN, DEFAULT_DOCUMENT_TYPE)));
        supplier.setDocumentNumber(stripExcelDecimal(cellAt(supplierCells, DOCUMENT_NUMBER_COLUMN, EMPTY_TEXT)));
        supplier.setBusinessName(TextNormalizer.trim(cellAt(supplierCells, BUSINESS_NAME_COLUMN, EMPTY_TEXT)));
    }

    private void applyContact(SupplierDTO supplier, String[] supplierCells, boolean excelFormat) {
        supplier.setContactName(TextNormalizer.trim(cellAt(supplierCells, CONTACT_NAME_COLUMN, EMPTY_TEXT)));
        supplier.setPhone(phoneNumber(supplierCells, excelFormat));
        supplier.setEmail(TextNormalizer.email(cellAt(supplierCells, EMAIL_COLUMN, EMPTY_TEXT)));
        supplier.setAddress(TextNormalizer.trim(cellAt(supplierCells, ADDRESS_COLUMN, EMPTY_TEXT)));
        supplier.setGeographicLocationId(
                parseGeographicLocation(cellAt(supplierCells, GEOGRAPHIC_LOCATION_COLUMN, null)));
    }

    private void applyClassification(SupplierDTO supplier, String[] supplierCells, String categoryPattern) {
        String categoriesText = cellAt(supplierCells, PRODUCT_CATEGORIES_COLUMN, EMPTY_TEXT);
        supplier.setProductCategories(parseCategories(categoriesText, categoryPattern));
        supplier.setCertifications(parseCertifications(supplierCells));
    }

    // A missing phone or UBIGEO is left empty so the row is reported, never filled with invented data.
    private String phoneNumber(String[] supplierCells, boolean excelFormat) {
        String phone = cellAt(supplierCells, PHONE_COLUMN, null);
        if (phone == null) {
            return EMPTY_TEXT;
        }
        return excelFormat ? stripExcelDecimal(phone) : phone;
    }

    private String cellAt(String[] supplierCells, int columnIndex, String defaultText) {
        return columnIndex < supplierCells.length && supplierCells[columnIndex] != null
                ? supplierCells[columnIndex] : defaultText;
    }

    private String blankToNull(String text) {
        return text.isBlank() ? null : text.trim();
    }

    private String stripExcelDecimal(String numericText) {
        return TextNormalizer.stripExcelDecimal(numericText);
    }

    private boolean isMissing(String text) {
        return text == null || text.isBlank() || MISSING_VALUE_PLACEHOLDER.equals(text.trim());
    }

    private Integer parseGeographicLocation(String locationIdText) {
        if (isMissing(locationIdText)) {
            return null;
        }
        try {
            return Integer.parseInt(stripExcelDecimal(locationIdText));
        } catch (NumberFormatException numberFormatException) {
            log.warn("Invalid geographicLocation value in supplier import: {}", locationIdText);
            return INVALID_GEOGRAPHIC_LOCATION_ID;
        }
    }

    private Map<String, Object> parseCertifications(String[] supplierCells) {
        String isoCertified = cellAt(supplierCells, ISO_CERTIFICATION_COLUMN, DEFAULT_ISO_CERTIFICATION);
        String registration = cellAt(supplierCells, SENASA_REGISTRATION_COLUMN, EMPTY_TEXT);
        return Map.of(ISO_CERTIFICATION_KEY, Boolean.parseBoolean(isoCertified),
                SENASA_REGISTRATION_KEY, isMissing(registration) ? EMPTY_TEXT : registration);
    }

    private void createCell(Row row, int column, String cellText, CellStyle style) {
        ExcelExportSupport.createCell(row, column, cellText, style);
    }

    private String trimOrEmpty(String text) {
        return text == null ? EMPTY_TEXT : text.trim();
    }

    private List<String> parseCategories(String categoriesText, String categoryPattern) {
        List<String> categories = new ArrayList<>();
        if (isMissing(categoriesText)) {
            return categories;
        }
        for (String category : categoriesText.split(categoryPattern)) {
            String normalizedCategory = category.trim();
            if (!normalizedCategory.isEmpty()) {
                categories.add(normalizedCategory);
            }
        }
        return categories;
    }
}
