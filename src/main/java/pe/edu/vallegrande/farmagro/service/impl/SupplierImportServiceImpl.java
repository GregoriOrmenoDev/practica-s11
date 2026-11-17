package pe.edu.vallegrande.farmagro.service.impl;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.farmagro.dto.ImportResultDTO;
import pe.edu.vallegrande.farmagro.dto.SupplierDTO;
import pe.edu.vallegrande.farmagro.service.SupplierExcelService;
import pe.edu.vallegrande.farmagro.service.SupplierImportService;
import pe.edu.vallegrande.farmagro.service.SupplierService;
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
public class SupplierImportServiceImpl implements SupplierImportService {

    private static final int IMPORTED_COUNT_INCREMENT = 1;
    private static final int FIRST_DATA_ROW_NUMBER = 2;

    private final SupplierService supplierService;
    private final SupplierExcelService supplierExcelService;
    private final Validator validator;

    @Override
    public Mono<ImportResultDTO> importSuppliers(FilePart file) {
        if (file == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe seleccionar un archivo para importar"));
        }
        return readSupplierFile(file)
                .flatMap(this::saveSupplierRows)
                .onErrorMap(this::asBadRequest);
    }

    private Mono<List<SupplierDTO>> readSupplierFile(FilePart file) {
        return MasterImportFiles.joinContent(file).flatMap(buffer -> {
            byte[] fileBytes = MasterImportFiles.readBytesAndRelease(buffer);
            boolean csvFile = MasterImportFiles.isCsv(file);
            return Mono.using(() -> new ByteArrayInputStream(fileBytes), input ->
                    (csvFile ? supplierExcelService.parseCsv(input) : supplierExcelService.parseExcel(input))
                            .collectList(), input -> { });
        });
    }

    private Mono<ImportResultDTO> saveSupplierRows(List<SupplierDTO> suppliers) {
        if (suppliers.isEmpty()) {
            return Mono.just(ImportResultDTO.builder()
                    .errors(List.of("El archivo no contiene registros válidos para importar"))
                    .build());
        }
        ImportResultDTO result = ImportResultDTO.builder().totalRows(suppliers.size()).build();
        return Flux.fromIterable(suppliers)
                .index()
                .concatMap(row -> saveSupplierRow(row.getT1() + FIRST_DATA_ROW_NUMBER, row.getT2(), result))
                .then(Mono.just(result));
    }

    private Mono<Void> saveSupplierRow(long rowNumber, SupplierDTO supplier, ImportResultDTO result) {
        return ImportDtoValidation.validate(validator, supplier)
                .then(supplierService.create(supplier))
                .doOnNext(saved -> {
                    result.setImportedCount(result.getImportedCount() + IMPORTED_COUNT_INCREMENT);
                    result.getImported().add(saved);
                })
                .then()
                .onErrorResume(error -> {
                    result.setErrorCount(result.getErrorCount() + IMPORTED_COUNT_INCREMENT);
                    result.getErrors().add("Fila " + rowNumber + ": " + ImportDtoValidation.reason(error));
                    return Mono.empty();
                });
    }

    private ResponseStatusException asBadRequest(Throwable error) {
        if (error instanceof ResponseStatusException statusException) {
            return statusException;
        }
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Error al procesar el archivo: " + error.getMessage(), error);
    }


}
