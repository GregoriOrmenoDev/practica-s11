package pe.edu.vallegrande.farmagro.service.impl;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import pe.edu.vallegrande.farmagro.util.AppConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

final class ExcelExportSupport {

    private static final float HEADER_ROW_HEIGHT = 26;
    private static final float DATA_ROW_HEIGHT = 20;
    private static final short HEADER_FONT_SIZE = 11;
    private static final int HEADER_ROW_INDEX = 0;
    private static final int FIRST_DATA_ROW_INDEX = HEADER_ROW_INDEX + 1;
    private static final int ALTERNATE_ROW_INTERVAL = 2;

    @FunctionalInterface
    interface DataRowWriter<T> {
        void write(Row row, T item, CellStyle style);
    }

    private record RowStyles(CellStyle regularRowStyle, CellStyle alternateRowStyle) {
    }

    private ExcelExportSupport() {
    }

    static <T> byte[] createWorkbook(String sheetName, String[] headers, List<T> items,
                                     DataRowWriter<T> rowWriter) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            RowStyles rowStyles = new RowStyles(dataStyle, createAlternateStyle(workbook, dataStyle));

            writeHeaderRow(sheet, headers, headerStyle);
            writeDataRows(sheet, items, rowStyles, rowWriter);
            autoSizeColumns(sheet, headers.length);

            workbook.write(output);
            return output.toByteArray();
        }
    }

    static void createCell(Row row, int columnIndex, String cellText, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(cellText != null ? cellText : AppConstants.EMPTY_TEXT);
        cell.setCellStyle(style);
    }

    private static void writeHeaderRow(Sheet sheet, String[] headers, CellStyle style) {
        Row row = sheet.createRow(HEADER_ROW_INDEX);
        row.setHeightInPoints(HEADER_ROW_HEIGHT);
        for (int columnIndex = 0; columnIndex < headers.length; columnIndex++) {
            createCell(row, columnIndex, headers[columnIndex], style);
        }
    }

    private static <T> void writeDataRows(Sheet sheet, List<T> items, RowStyles rowStyles,
                                          DataRowWriter<T> rowWriter) {
        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            int rowIndex = itemIndex + FIRST_DATA_ROW_INDEX;
            Row row = sheet.createRow(rowIndex);
            row.setHeightInPoints(DATA_ROW_HEIGHT);
            CellStyle rowStyle = rowIndex % ALTERNATE_ROW_INTERVAL == 0 ? rowStyles.alternateRowStyle() : rowStyles.regularRowStyle();
            rowWriter.write(row, items.get(itemIndex), rowStyle);
        }
    }

    private static void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            sheet.autoSizeColumn(columnIndex);
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints(HEADER_FONT_SIZE);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private static CellStyle createAlternateStyle(Workbook workbook, CellStyle dataStyle) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(dataStyle);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static void setBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
