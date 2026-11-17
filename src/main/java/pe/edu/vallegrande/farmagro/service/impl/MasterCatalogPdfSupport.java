package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.util.AppConstants;

import pe.edu.vallegrande.farmagro.util.PdfConstants;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.BiFunction;

final class MasterCatalogPdfSupport {

    private static final String UNKNOWN_DOCUMENT_TYPE = "DOC";
    private static final String WORD_SEPARATOR = " ";
    private static final String ACTIVE_STATUS_LABEL = "Activo";
    private static final String INACTIVE_STATUS_LABEL = "Inactivo";

    static final Color BRAND_GREEN = new Color(45, 106, 79);
    static final Color BRAND_LIGHT = new Color(243, 244, 246);
    static final Color BORDER_COLOR = new Color(229, 231, 235);

    record TableLayout(float[] columnWidths, String[] headers) {
    }

    record ReportSpec(String title, BiFunction<String, Integer, String> subtitleBuilder,
                      int recordCount, TableLayout layout) {
    }

    record CatalogRow(PdfPTable table, Color background) {
    }

    record RowFonts(Font regular, Font emphasized) {
    }

    private static final PdfDocumentSupport.PageMargins PAGE_MARGINS = new PdfDocumentSupport.PageMargins(
            PdfConstants.MASTER_CATALOG_MARGIN_LEFT, PdfConstants.MASTER_CATALOG_MARGIN_RIGHT,
            PdfConstants.MASTER_CATALOG_MARGIN_TOP, PdfConstants.MASTER_CATALOG_MARGIN_BOTTOM);
    private static final PdfTableSupport.CellBorder BODY_CELL_BORDER =
            new PdfTableSupport.CellBorder(PdfConstants.REPORT_BODY_CELL_PADDING, BORDER_COLOR);
    private static final PdfTableSupport.CellBorder HEADER_CELL_BORDER =
            new PdfTableSupport.CellBorder(PdfConstants.REPORT_HEADER_CELL_PADDING, Color.WHITE);

    @FunctionalInterface
    interface RowRenderer {
        void render(PdfPTable table, int rowIndex);
    }

    private MasterCatalogPdfSupport() {
    }

    static byte[] generate(ReportSpec spec, RowRenderer rowRenderer) throws Exception {
        return PdfDocumentSupport.generate(PAGE_MARGINS, document -> {
            addHeader(document, spec);
            PdfPTable table = createTable(spec);
            for (int rowIndex = PdfConstants.FIRST_ROW_INDEX; rowIndex < spec.recordCount(); rowIndex++) {
                rowRenderer.render(table, rowIndex);
            }
            document.add(table);
            addFooter(document);
        });
    }

    static void addCell(CatalogRow row, String text, Font font, int alignment) {
        PdfTableSupport.addCell(row.table(), text, new PdfTableSupport.CellAppearance(
                font, row.background(), alignment, BODY_CELL_BORDER));
    }

    static CatalogRow catalogRow(PdfPTable table, int rowIndex) {
        return new CatalogRow(table, rowBackground(rowIndex));
    }

    static RowFonts rowFonts() {
        return new RowFonts(regularFont(), emphasizedFont());
    }

    static String valueOrDash(String cellText) {
        return cellText == null ? AppConstants.MISSING_VALUE_PLACEHOLDER : cellText;
    }

    static String documentLabel(String documentType, String documentNumber) {
        return Objects.requireNonNullElse(documentType, UNKNOWN_DOCUMENT_TYPE) + WORD_SEPARATOR
                + valueOrDash(documentNumber);
    }

    static String statusLabel(Boolean status) {
        return Boolean.TRUE.equals(status) ? ACTIVE_STATUS_LABEL : INACTIVE_STATUS_LABEL;
    }

    static Color rowBackground(int rowIndex) {
        return rowIndex % PdfConstants.ALTERNATING_ROW_INTERVAL == 0 ? Color.WHITE : BRAND_LIGHT;
    }

    static Font regularFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, PdfConstants.REPORT_ROW_FONT_SIZE, Color.BLACK);
    }

    static Font emphasizedFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, PdfConstants.REPORT_ROW_FONT_SIZE, Color.BLACK);
    }

    private static void addHeader(Document document, ReportSpec spec) throws DocumentException {
        Font titleFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, PdfConstants.REPORT_TITLE_FONT_SIZE, BRAND_GREEN);
        Paragraph title = new Paragraph(spec.title(), titleFont);
        title.setAlignment(Element.ALIGN_LEFT);
        title.setSpacingAfter(PdfConstants.REPORT_TITLE_SPACING);
        document.add(title);

        String generatedAt = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern(PdfConstants.REPORT_TIMESTAMP_PATTERN));
        Font subtitleFont = FontFactory.getFont(
                FontFactory.HELVETICA, PdfConstants.REPORT_SUBTITLE_FONT_SIZE, Color.DARK_GRAY);
        Paragraph subtitle = new Paragraph(
                spec.subtitleBuilder().apply(generatedAt, spec.recordCount()), subtitleFont);
        subtitle.setSpacingAfter(PdfConstants.REPORT_SUBTITLE_SPACING);
        document.add(subtitle);
    }

    private static PdfPTable createTable(ReportSpec spec) throws DocumentException {
        PdfPTable table = new PdfPTable(PdfConstants.MASTER_CATALOG_TABLE_COLUMNS);
        table.setWidthPercentage(PdfConstants.TABLE_WIDTH_PERCENTAGE);
        table.setWidths(spec.layout().columnWidths());
        Font headerFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, PdfConstants.REPORT_TABLE_HEADER_FONT_SIZE, Color.WHITE);
        for (String header : spec.layout().headers()) {
            PdfTableSupport.addCell(table, header, new PdfTableSupport.CellAppearance(
                    headerFont, BRAND_GREEN, Element.ALIGN_CENTER, HEADER_CELL_BORDER));
        }
        return table;
    }

    private static void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph("\nSistema de Gestión Comercial y Logística FarmaGro S4",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,
                        PdfConstants.REPORT_FOOTER_FONT_SIZE, Color.GRAY));
        footer.setAlignment(Element.ALIGN_RIGHT);
        document.add(footer);
    }
}
