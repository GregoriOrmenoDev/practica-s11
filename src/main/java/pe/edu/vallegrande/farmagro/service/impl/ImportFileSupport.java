package pe.edu.vallegrande.farmagro.service.impl;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class ImportFileSupport {

    private ImportFileSupport() {
    }

    static <T> Flux<T> parseExcel(InputStream input, int firstWorksheetIndex, int firstDataRowIndex,
                                  Function<Row, T> rowMapper) {
        return Mono.fromCallable(() -> {
            List<T> items = new ArrayList<>();
            try (Workbook workbook = new XSSFWorkbook(input)) {
                Sheet sheet = workbook.getSheetAt(firstWorksheetIndex);
                for (int rowIndex = firstDataRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }
                    T item = rowMapper.apply(row);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
            return items;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(Flux::fromIterable);
    }

    static <T> Flux<T> parseCsv(InputStream input, int firstDataRowIndex, int minimumFields,
                                Function<String[], T> rowMapper) {
        return Mono.fromCallable(() -> {
            List<String[]> records = CsvRecordParser.readAll(input);
            List<T> items = new ArrayList<>();
            for (int rowIndex = firstDataRowIndex; rowIndex < records.size(); rowIndex++) {
                String[] fields = records.get(rowIndex);
                if (CsvRecordParser.isBlank(fields) || fields.length < minimumFields) {
                    continue;
                }
                T item = rowMapper.apply(fields);
                if (item != null) {
                    items.add(item);
                }
            }
            return items;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(Flux::fromIterable);
    }

    static <T> Flux<T> parseCsvResource(String resourcePath, Function<InputStream, Flux<T>> csvParser) {
        return Mono.fromCallable(() -> new ClassPathResource(resourcePath).getInputStream())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(csvParser);
    }

    static String getCellValue(Cell cell) {
        return cellValue(cell, false);
    }

    static String getIntegerCellValue(Cell cell) {
        return cellValue(cell, true);
    }

    private static String cellValue(Cell cell, boolean integerNumericCells) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> integerNumericCells
                    ? String.valueOf((long) cell.getNumericCellValue())
                    : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
