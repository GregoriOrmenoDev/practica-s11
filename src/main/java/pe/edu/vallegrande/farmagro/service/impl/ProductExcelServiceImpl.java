package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.dto.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class ProductExcelServiceImpl implements pe.edu.vallegrande.farmagro.service.ProductExcelService {

    private static final String TEMPLATE_RESOURCE = "templates/product-template.csv";
    private static final String WORKBOOK_TITLE = "Products FarmaGro";
    private static final String DEFAULT_TOXICITY_LEVEL = AppConstants.DEFAULT_TOXICITY_LEVEL;
    private static final String DEFAULT_LICENSE_REQUIRED = "false";
    private static final String DEFAULT_PRICE_TEXT = "0.00";
    private static final double DEFAULT_PRICE = 0.0;
    private static final String DEFAULT_BRAND = "Sin marca";
    private static final String DEFAULT_CATEGORY = "General";
    private static final String EXCEL_IMPORT_DESCRIPTION = "Producto importado desde Excel";
    private static final String CSV_IMPORT_DESCRIPTION = "Producto importado desde CSV";
    private static final String EMPTY_TEXT = AppConstants.EMPTY_TEXT;
    private static final String CATEGORY_DELIMITER = "|";
    private static final String CATEGORY_DELIMITER_PATTERN = "\\|";
    private static final String DECIMAL_COMMA = ",";
    private static final String DECIMAL_POINT = ".";
    private static final int FIRST_WORKSHEET_INDEX = 0;
    private static final int FIRST_DATA_ROW_INDEX = 1;
    private static final int MINIMUM_CSV_FIELDS = 2;

    private static final int PRODUCT_ID_COLUMN = 0;
    private static final int PRODUCT_NAME_COLUMN = 1;
    private static final int PRODUCT_BRAND_COLUMN = 2;
    private static final int HEALTH_REGISTRATION_COLUMN = 3;
    private static final int DESCRIPTION_COLUMN = 4;
    private static final int ACTIVE_INGREDIENT_COLUMN = 5;
    private static final int TOXICITY_LEVEL_COLUMN = 6;
    private static final int LICENSE_REQUIRED_COLUMN = 7;
    private static final int CATEGORIES_COLUMN = 8;
    private static final int SALE_PRICE_COLUMN = 9;
    private static final int IMAGE_URL_COLUMN = 10;

    private static final String[] HEADERS = {
            "ID (Optional)",
            "Name",
            "Brand",
            "Health Registration",
            "Description",
            "Active Ingredient",
            "Toxicity Level (I/II/III/IV)",
            "Requires License (true/false)",
            "Categories (separated by |)",
            "Sale Price",
            "Image URL (Optional)"
    };

    public Mono<byte[]> exportToExcel(List<ProductDTO> products) {
        return Mono.fromCallable(() -> ExcelExportSupport.createWorkbook(
                        WORKBOOK_TITLE, HEADERS, products, this::writeProductRow))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void writeProductRow(Row row, ProductDTO product, CellStyle style) {
        createCell(row, PRODUCT_ID_COLUMN, textOrDefault(product.getId(), EMPTY_TEXT), style);
        createCell(row, PRODUCT_NAME_COLUMN, textOrDefault(product.getName(), EMPTY_TEXT), style);
        createCell(row, PRODUCT_BRAND_COLUMN, textOrDefault(product.getBrand(), EMPTY_TEXT), style);
        createCell(row, HEALTH_REGISTRATION_COLUMN,
                textOrDefault(product.getSanitaryRegistration(), EMPTY_TEXT), style);
        createCell(row, DESCRIPTION_COLUMN, textOrDefault(product.getDescription(), EMPTY_TEXT), style);
        createCell(row, ACTIVE_INGREDIENT_COLUMN, textOrDefault(product.getActiveIngredient(), EMPTY_TEXT), style);
        createCell(row, TOXICITY_LEVEL_COLUMN,
                textOrDefault(product.getToxicityLevel(), DEFAULT_TOXICITY_LEVEL), style);
        createCell(row, LICENSE_REQUIRED_COLUMN, licenseText(product.getRequiresLicense()), style);
        createCell(row, CATEGORIES_COLUMN, joinCategories(product.getCategories()), style);
        createCell(row, SALE_PRICE_COLUMN, priceText(product.getPrice()), style);
        createCell(row, IMAGE_URL_COLUMN, textOrDefault(product.getImageUrl(), EMPTY_TEXT), style);
    }

    private String licenseText(Boolean requiresLicense) {
        return requiresLicense == null ? DEFAULT_LICENSE_REQUIRED : String.valueOf(requiresLicense);
    }

    private String joinCategories(List<String> categories) {
        return categories == null ? EMPTY_TEXT : String.join(CATEGORY_DELIMITER, categories);
    }

    private String priceText(Double price) {
        return price == null ? DEFAULT_PRICE_TEXT : String.valueOf(price);
    }

    public Mono<byte[]> generateTemplate() {
        return ImportFileSupport.parseCsvResource(TEMPLATE_RESOURCE, this::parseCsv)
                .collectList()
                .flatMap(this::exportToExcel);
    }

    public Flux<ProductDTO> parseExcel(InputStream input) {
        return ImportFileSupport.parseExcel(input, FIRST_WORKSHEET_INDEX, FIRST_DATA_ROW_INDEX,
                row -> buildProductDto(readExcelCells(row), EXCEL_IMPORT_DESCRIPTION));
    }

    private String[] readExcelCells(Row row) {
        String[] productCells = new String[HEADERS.length];
        for (int columnIndex = 0; columnIndex < productCells.length; columnIndex++) {
            productCells[columnIndex] = ImportFileSupport.getCellValue(row.getCell(columnIndex));
        }
        return productCells;
    }

    public Flux<ProductDTO> parseCsv(InputStream input) {
        return ImportFileSupport.parseCsv(input, FIRST_DATA_ROW_INDEX, MINIMUM_CSV_FIELDS, this::parseCsvRecord);
    }

    private ProductDTO parseCsvRecord(String[] csvFields) {
        String[] productCells = new String[HEADERS.length];
        for (int columnIndex = 0; columnIndex < productCells.length; columnIndex++) {
            productCells[columnIndex] = columnIndex < csvFields.length ? trimOrNull(csvFields[columnIndex]) : null;
        }
        return buildProductDto(productCells, CSV_IMPORT_DESCRIPTION);
    }

    private ProductDTO buildProductDto(String[] productCells, String defaultDescription) {
        String productName = blankToNull(productCells[PRODUCT_NAME_COLUMN]);
        if (productName == null) {
            return null;
        }
        ProductDTO product = ProductDTO.builder()
                .id(blankToNull(productCells[PRODUCT_ID_COLUMN]))
                .name(productName)
                .build();
        applyLabeling(product, productCells, defaultDescription);
        applyRegulation(product, productCells);
        applyCommercialData(product, productCells);
        return product;
    }

    private void applyLabeling(ProductDTO product, String[] productCells, String defaultDescription) {
        product.setBrand(textOrDefault(blankToNull(productCells[PRODUCT_BRAND_COLUMN]), DEFAULT_BRAND));
        product.setDescription(textOrDefault(blankToNull(productCells[DESCRIPTION_COLUMN]), defaultDescription));
        product.setImageUrl(blankToNull(productCells[IMAGE_URL_COLUMN]));
    }

    private void applyRegulation(ProductDTO product, String[] productCells) {
        product.setSanitaryRegistration(blankToNull(productCells[HEALTH_REGISTRATION_COLUMN]));
        product.setActiveIngredient(blankToNull(productCells[ACTIVE_INGREDIENT_COLUMN]));
        product.setToxicityLevel(normalizeToxicity(productCells[TOXICITY_LEVEL_COLUMN]));
        product.setRequiresLicense(Boolean.parseBoolean(trimOrNull(productCells[LICENSE_REQUIRED_COLUMN])));
    }

    private void applyCommercialData(ProductDTO product, String[] productCells) {
        product.setCategories(parseCategories(productCells[CATEGORIES_COLUMN]));
        product.setPrice(parsePrice(productCells[SALE_PRICE_COLUMN]));
    }

    private String textOrDefault(String text, String defaultText) {
        return text == null ? defaultText : text;
    }

    private String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private String trimOrNull(String text) {
        return text == null ? null : text.trim();
    }

    // Blank keeps the same default as the web form; an unknown value is passed on so the row is
    // rejected by the DTO validation instead of silently becoming the least toxic category (IV).
    private String normalizeToxicity(String toxicity) {
        String normalizedToxicity = blankToNull(toxicity);
        if (normalizedToxicity == null) return DEFAULT_TOXICITY_LEVEL;
        return normalizedToxicity.toUpperCase(Locale.ROOT);
    }

    private List<String> parseCategories(String categoriesText) {
        List<String> categories = new ArrayList<>();
        if (categoriesText != null && !categoriesText.isBlank()) {
            for (String rawCategory : categoriesText.split(CATEGORY_DELIMITER_PATTERN)) {
                String categoryName = rawCategory.trim();
                if (!categoryName.isEmpty()) categories.add(categoryName);
            }
        }
        if (categories.isEmpty()) categories.add(DEFAULT_CATEGORY);
        return categories;
    }

    private Double parsePrice(String priceText) {
        String normalizedPrice = blankToNull(priceText);
        if (normalizedPrice == null) {
            return DEFAULT_PRICE;
        }
        try {
            return Double.parseDouble(normalizedPrice.replace(DECIMAL_COMMA, DECIMAL_POINT));
        } catch (NumberFormatException numberFormatException) {
            log.warn("Invalid product price format: {}", priceText);
            return DEFAULT_PRICE;
        }
    }

    private void createCell(Row row, int column, String cellText, CellStyle style) {
        ExcelExportSupport.createCell(row, column, cellText, style);
    }
}
