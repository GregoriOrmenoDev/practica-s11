package pe.edu.vallegrande.farmagro.rest;

import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.dto.ProductDTO;
import pe.edu.vallegrande.farmagro.dto.ImportResultDTO;
import pe.edu.vallegrande.farmagro.service.ProductExportService;
import pe.edu.vallegrande.farmagro.service.ProductImportService;
import pe.edu.vallegrande.farmagro.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductRest {

    private final ProductService productService;
    private final ProductImportService productImportService;
    private final ProductExportService productExportService;

    @GetMapping
    public Flux<ProductDTO> getAll(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_LIST_PAGE_SIZE_VALUE) int size) {
        return productService.findAll(page, size);
    }

    @GetMapping("/active")
    public Flux<ProductDTO> getAllActive(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_LIST_PAGE_SIZE_VALUE) int size) {
        return productService.findAllActive(page, size);
    }

    @GetMapping("/deleted")
    public Flux<ProductDTO> getAllDeleted(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_LIST_PAGE_SIZE_VALUE) int size) {
        return productService.findAllDeleted(page, size);
    }

    @GetMapping("/category/{category}")
    public Flux<ProductDTO> getByCategory(@PathVariable String category) {
        return productService.findByCategory(category);
    }

    @GetMapping("/{id}")
    public Mono<ProductDTO> getById(@PathVariable String id) {
        return productService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProductDTO> create(@Valid @RequestBody ProductDTO dto) {
        return productService.create(dto);
    }

    @PutMapping("/{id}")
    public Mono<ProductDTO> update(@PathVariable String id,
                                   @Valid @RequestBody ProductDTO dto) {
        return productService.update(id, dto);
    }

    @PatchMapping("/{id}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable String id) {
        return productService.delete(id);
    }

    @PatchMapping("/{id}/restore")
    public Mono<ProductDTO> restore(@PathVariable String id) {
        return productService.restore(id);
    }

    @GetMapping("/export/excel")
    public Mono<ResponseEntity<byte[]>> exportExcel() {
        return productExportService.exportExcel()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"products_farmagro.xlsx\"")
                        .contentType(MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(bytes));
    }

    @GetMapping("/export/pdf")
    public Mono<ResponseEntity<byte[]>> exportPdf() {
        return productExportService.exportPdf()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"products_farmagro.pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(bytes));
    }

    @GetMapping("/template/excel")
    public Mono<ResponseEntity<byte[]>> getTemplate() {
        return productExportService.getExcelTemplate()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"products_template_farmagro.xlsx\"")
                        .contentType(MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(bytes));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ImportResultDTO> importProducts(@RequestPart("file") FilePart file) {
        return productImportService.importProducts(file);
    }
}
