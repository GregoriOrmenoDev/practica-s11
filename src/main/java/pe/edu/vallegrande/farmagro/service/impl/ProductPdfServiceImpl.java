package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.util.AppConstants;

import pe.edu.vallegrande.farmagro.dto.ProductDTO;
import pe.edu.vallegrande.farmagro.util.PdfConstants;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ProductPdfServiceImpl implements pe.edu.vallegrande.farmagro.service.ProductPdfService {

    private static final String LICENSE_REQUIRED_LABEL = "✔ Sí";
    private static final String LICENSE_NOT_REQUIRED_LABEL = "No";
    private static final String CATEGORY_SEPARATOR = ", ";
    private static final String CURRENCY_PREFIX = "S/ ";
    private static final String TOXICITY_CLASS_PREFIX = "Clase ";

    private static final PdfDocumentSupport.PageMargins PAGE_MARGINS = new PdfDocumentSupport.PageMargins(
            PdfConstants.PRODUCT_REPORT_MARGIN_LEFT, PdfConstants.PRODUCT_REPORT_MARGIN_RIGHT,
            PdfConstants.PRODUCT_REPORT_MARGIN_TOP, PdfConstants.PRODUCT_REPORT_MARGIN_BOTTOM);
    private static final PdfTableSupport.CellBorder HEADER_CELL_BORDER =
            new PdfTableSupport.CellBorder(PdfConstants.PRODUCT_HEADER_CELL_PADDING, Color.WHITE);
    private static final PdfTableSupport.CellBorder BODY_CELL_BORDER = new PdfTableSupport.CellBorder(
            PdfConstants.REPORT_BODY_CELL_PADDING, MasterCatalogPdfSupport.BORDER_COLOR);

    private static final Color TOX_RED      = new Color(214, 40, 40);
    private static final Color TOX_ORANGE   = new Color(244, 162, 97);
    private static final Color TOX_YELLOW   = new Color(233, 196, 106);
    private static final Color TOX_GREEN    = new Color(82, 183, 136);

    private static final float COLUMN_WIDTH_ID = 1.8f;
    private static final float COLUMN_WIDTH_NAME = 4.5f;
    private static final float COLUMN_WIDTH_BRAND = 2.8f;
    private static final float COLUMN_WIDTH_REGISTRY = 3.2f;
    private static final float COLUMN_WIDTH_INGREDIENT = 2.8f;
    private static final float COLUMN_WIDTH_TOXICITY = 1.5f;
    private static final float COLUMN_WIDTH_LICENSE = 1.8f;
    private static final float COLUMN_WIDTH_CATEGORIES = 3.0f;
    private static final float COLUMN_WIDTH_PRICE = 1.8f;

    @Override
    public Mono<byte[]> exportToPdf(List<ProductDTO> products) {
        return Mono.fromCallable(() -> generatePdf(products))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private byte[] generatePdf(List<ProductDTO> products) throws Exception {
        return PdfDocumentSupport.generate(PAGE_MARGINS, document -> writeProductReport(document, products));
    }

    private void writeProductReport(Document document, List<ProductDTO> products) throws DocumentException {
        addDocumentHeader(document, products.size());
        PdfPTable table = createProductTable();
        populateTableWithData(table, products);
        document.add(table);
        addDocumentFooter(document);
    }

    private void addDocumentHeader(Document document, int totalRecords) throws DocumentException {
        Font titleFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, PdfConstants.PRODUCT_REPORT_TITLE_FONT_SIZE, MasterCatalogPdfSupport.BRAND_GREEN);
        Paragraph title = new Paragraph("🌱 FARMAGRO S.A.C. - AGROCHEMICAL PRODUCTS MASTER CATALOG", titleFont);
        title.setAlignment(Element.ALIGN_LEFT);
        title.setSpacingAfter(PdfConstants.REPORT_TITLE_SPACING);
        document.add(title);

        Font subtitleFont = FontFactory.getFont(
                FontFactory.HELVETICA, PdfConstants.PRODUCT_REPORT_SUBTITLE_FONT_SIZE, Color.DARK_GRAY);
        String date = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern(PdfConstants.REPORT_TIMESTAMP_PATTERN));
        Paragraph subtitle = new Paragraph(
                "Official product catalog | Issued on: " + date +
                " | Total records: " + totalRecords, subtitleFont);
        subtitle.setSpacingAfter(PdfConstants.PRODUCT_SUBTITLE_SPACING);
        document.add(subtitle);

        addToxicityLegend(document);
    }

    private void addToxicityLegend(Document document) throws DocumentException {
        Font legendFont = FontFactory.getFont(
                FontFactory.HELVETICA_OBLIQUE, PdfConstants.PRODUCT_LEGEND_FONT_SIZE, Color.DARK_GRAY);
        Paragraph legend = new Paragraph(
                "Niveles de toxicidad OMS:  Clase I = Extremadamente tóxico  |  " +
                "Clase II = Altamente tóxico  |  Clase III = Moderadamente tóxico  |  " +
                "Clase IV = Ligeramente tóxico", legendFont);
        legend.setSpacingAfter(PdfConstants.PRODUCT_LEGEND_SPACING);
        document.add(legend);
    }

    private PdfPTable createProductTable() throws DocumentException {
        PdfPTable table = new PdfPTable(PdfConstants.PRODUCT_TABLE_COLUMNS);
        table.setWidthPercentage(PdfConstants.TABLE_WIDTH_PERCENTAGE);
        table.setWidths(new float[]{
                COLUMN_WIDTH_ID,
                COLUMN_WIDTH_NAME,
                COLUMN_WIDTH_BRAND,
                COLUMN_WIDTH_REGISTRY,
                COLUMN_WIDTH_INGREDIENT,
                COLUMN_WIDTH_TOXICITY,
                COLUMN_WIDTH_LICENSE,
                COLUMN_WIDTH_CATEGORIES,
                COLUMN_WIDTH_PRICE
        });

        addTableHeaders(table);
        return table;
    }

    private void addTableHeaders(PdfPTable table) {
        String[] headers = {
                "ID", "Nombre", "Marca", "Reg. Sanitario",
                "Principio Activo", "Toxicidad", "Licencia",
                "Categorías", "Precio"
        };
        Font headerFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, PdfConstants.PRODUCT_REPORT_TABLE_HEADER_FONT_SIZE, Color.WHITE);
        for (String header : headers) {
            PdfTableSupport.addCell(table, header, new PdfTableSupport.CellAppearance(
                    headerFont, MasterCatalogPdfSupport.BRAND_GREEN, Element.ALIGN_CENTER, HEADER_CELL_BORDER));
        }
    }

    private void populateTableWithData(PdfPTable table, List<ProductDTO> products) {
        ProductRowStyles styles = new ProductRowStyles(
                FontFactory.getFont(FontFactory.HELVETICA, PdfConstants.PRODUCT_REPORT_ROW_FONT_SIZE, Color.BLACK),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, PdfConstants.PRODUCT_REPORT_ROW_FONT_SIZE, Color.BLACK),
                FontFactory.getFont(FontFactory.HELVETICA, PdfConstants.PRODUCT_REPORT_SMALL_FONT_SIZE, Color.DARK_GRAY));

        int rowIndex = PdfConstants.FIRST_ROW_INDEX;
        for (ProductDTO product : products) {
            addProductRow(MasterCatalogPdfSupport.catalogRow(table, rowIndex), product, styles);
            rowIndex++;
        }
    }

    private void addProductRow(MasterCatalogPdfSupport.CatalogRow row, ProductDTO product, ProductRowStyles styles) {
        ProductRowValues productRow = productRowValues(product);
        MasterCatalogPdfSupport.addCell(row, productRow.identity().id(), styles.bold(), Element.ALIGN_CENTER);
        MasterCatalogPdfSupport.addCell(row, productRow.identity().name(), styles.bold(), Element.ALIGN_LEFT);
        MasterCatalogPdfSupport.addCell(row, productRow.identity().brand(), styles.text(), Element.ALIGN_CENTER);
        MasterCatalogPdfSupport.addCell(row, productRow.details().sanitaryRegistration(), styles.small(), Element.ALIGN_CENTER);
        MasterCatalogPdfSupport.addCell(row, productRow.details().activeIngredient(), styles.small(), Element.ALIGN_LEFT);

        addToxicityCell(row.table(), product.getToxicityLevel());

        MasterCatalogPdfSupport.addCell(row, productRow.details().requiresLicense(), styles.text(), Element.ALIGN_CENTER);
        MasterCatalogPdfSupport.addCell(row, productRow.details().categories(), styles.small(), Element.ALIGN_LEFT);
        MasterCatalogPdfSupport.addCell(row, productRow.price(), styles.bold(), Element.ALIGN_RIGHT);
    }

    private ProductRowValues productRowValues(ProductDTO product) {
        return new ProductRowValues(
                new ProductIdentityCells(
                        valueOrDash(product.getId()),
                        valueOrDash(product.getName()),
                        valueOrDash(product.getBrand())),
                new ProductRowDetails(
                        valueOrDash(product.getSanitaryRegistration()),
                        valueOrDash(product.getActiveIngredient()),
                        Boolean.TRUE.equals(product.getRequiresLicense()) ? LICENSE_REQUIRED_LABEL : LICENSE_NOT_REQUIRED_LABEL,
                        product.getCategories() != null
                                ? String.join(CATEGORY_SEPARATOR, product.getCategories())
                                : AppConstants.MISSING_VALUE_PLACEHOLDER),
                product.getPrice() != null
                        ? CURRENCY_PREFIX + String.format(PdfConstants.PRODUCT_PRICE_FORMAT, product.getPrice())
                        : AppConstants.MISSING_VALUE_PLACEHOLDER);
    }

    private String valueOrDash(String cellText) {
        return MasterCatalogPdfSupport.valueOrDash(cellText);
    }

    private void addToxicityCell(PdfPTable table, String toxicityLevel) {
        Color toxColor = getToxColor(toxicityLevel);
        String toxLabel = toxicityLevel != null
                ? TOXICITY_CLASS_PREFIX + toxicityLevel : AppConstants.MISSING_VALUE_PLACEHOLDER;
        Font toxFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, PdfConstants.PRODUCT_REPORT_ROW_FONT_SIZE, Color.WHITE);
        PdfTableSupport.addCell(table, toxLabel, new PdfTableSupport.CellAppearance(
                toxFont, toxColor, Element.ALIGN_CENTER, BODY_CELL_BORDER));
    }

    private void addDocumentFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph(
                "\nFarmaGro S4 Commercial and Logistics Management System — Products Module",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,
                        PdfConstants.PRODUCT_REPORT_FOOTER_FONT_SIZE, Color.GRAY));
        footer.setAlignment(Element.ALIGN_RIGHT);
        document.add(footer);
    }


    private Color getToxColor(String level) {
        if (level == null) return Color.GRAY;
        return switch (level.toUpperCase()) {
            case "I"   -> TOX_RED;
            case "II"  -> TOX_ORANGE;
            case "III" -> TOX_YELLOW;
            default    -> TOX_GREEN;
        };
    }

    private record ProductRowStyles(Font text, Font bold, Font small) { }

    private record ProductRowDetails(String sanitaryRegistration, String activeIngredient,
                                     String requiresLicense, String categories) { }

    private record ProductIdentityCells(String id, String name, String brand) { }

    private record ProductRowValues(ProductIdentityCells identity, ProductRowDetails details, String price) { }

}
