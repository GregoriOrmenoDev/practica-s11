package pe.edu.vallegrande.farmagro.service.impl;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.farmagro.dto.ImportResultDTO;
import pe.edu.vallegrande.farmagro.dto.ProductDTO;
import pe.edu.vallegrande.farmagro.service.ProductExcelService;
import pe.edu.vallegrande.farmagro.service.ProductImportService;
import pe.edu.vallegrande.farmagro.service.ProductService;
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
public class ProductImportServiceImpl implements ProductImportService {

    private static final int COUNT_INCREMENT = 1;

    private final ProductService productService;
    private final ProductExcelService productExcelService;
    private final Validator validator;

    @Override
    public Mono<ImportResultDTO> importProducts(FilePart filePart) {
        if (filePart == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe seleccionar un archivo para importar"));
        }
        return readProductFile(filePart)
                .flatMap(this::importProductRows)
                .onErrorMap(this::asBadRequest);
    }

    private Mono<List<ProductDTO>> readProductFile(FilePart filePart) {
        return MasterImportFiles.joinContent(filePart).flatMap(buffer -> {
            byte[] fileBytes = MasterImportFiles.readBytesAndRelease(buffer);
            boolean csvFile = MasterImportFiles.isCsv(filePart);
            return Mono.using(() -> new ByteArrayInputStream(fileBytes), input ->
                    (csvFile ? productExcelService.parseCsv(input) : productExcelService.parseExcel(input))
                            .collectList(), input -> { });
        });
    }

    private Mono<ImportResultDTO> importProductRows(List<ProductDTO> products) {
        if (products.isEmpty()) {
            return Mono.just(ImportResultDTO.builder()
                    .errors(List.of("El archivo no contiene registros válidos para importar"))
                    .build());
        }
        ImportResultDTO result = ImportResultDTO.builder().totalRows(products.size()).build();
        return Flux.fromIterable(products)
                .concatMap(product -> importProductRow(product, result))
                .then(Mono.just(result));
    }

    private Mono<Void> importProductRow(ProductDTO product, ImportResultDTO result) {
        return ImportDtoValidation.validate(validator, product)
                .then(productService.create(product))
                .doOnNext(saved -> {
                    result.setImportedCount(result.getImportedCount() + COUNT_INCREMENT);
                    result.getImported().add(saved);
                })
                .then()
                .onErrorResume(error -> {
                    result.setErrorCount(result.getErrorCount() + COUNT_INCREMENT);
                    result.getErrors().add(productImportError(product, error));
                    return Mono.empty();
                });
    }

    private String productImportError(ProductDTO product, Throwable error) {
        return "Producto '" + product.getName() + "': " + ImportDtoValidation.reason(error);
    }

    private ResponseStatusException asBadRequest(Throwable error) {
        if (error instanceof ResponseStatusException statusException) {
            return statusException;
        }
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Error al procesar el archivo: " + error.getMessage(), error);
    }
}
