package pe.edu.vallegrande.farmagro.rest;

import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.dto.ImportResultDTO;
import pe.edu.vallegrande.farmagro.dto.SupplierDTO;
import pe.edu.vallegrande.farmagro.service.SupplierService;
import pe.edu.vallegrande.farmagro.service.SupplierExportService;
import pe.edu.vallegrande.farmagro.service.SupplierImportService;
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
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupplierRest {

    private final SupplierService supplierService;
    private final SupplierExportService supplierExportService;
    private final SupplierImportService supplierImportService;

    @GetMapping
    public Flux<SupplierDTO> getAll(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_LIST_PAGE_SIZE_VALUE) int size) {
        return supplierService.findAll(page, size);
    }

    @GetMapping("/status")
    public Flux<SupplierDTO> getByStatus(
            @RequestParam Boolean active,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_LIST_PAGE_SIZE_VALUE) int size) {
        return supplierService.findByStatus(active, page, size);
    }

    @GetMapping("/{id}")
    public Mono<SupplierDTO> getById(@PathVariable String id) {
        return supplierService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SupplierDTO> create(@Valid @RequestBody SupplierDTO dto) {
        return supplierService.create(dto);
    }

    @PutMapping("/{id}")
    public Mono<SupplierDTO> update(@PathVariable String id,
                                    @Valid @RequestBody SupplierDTO dto) {
        return supplierService.update(id, dto);
    }

    @PatchMapping("/{id}/delete")
    public Mono<SupplierDTO> delete(@PathVariable String id) {
        return supplierService.delete(id);
    }

    @PatchMapping("/{id}/restore")
    public Mono<SupplierDTO> restore(@PathVariable String id) {
        return supplierService.restore(id);
    }

    @GetMapping("/export/excel")
    public Mono<ResponseEntity<byte[]>> exportExcel() {
        return supplierExportService.exportExcel()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"suppliers_farmagro.xlsx\"")
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(bytes));
    }

    @GetMapping("/export/pdf")
    public Mono<ResponseEntity<byte[]>> exportPdf() {
        return supplierExportService.exportPdf()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"suppliers_farmagro.pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(bytes));
    }

    @GetMapping("/template/excel")
    public Mono<ResponseEntity<byte[]>> getTemplate() {
        return supplierExportService.getExcelTemplate()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla_suppliers_farmagro.xlsx\"")
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(bytes));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ImportResultDTO> importSuppliers(@RequestPart("file") FilePart file) {
        return supplierImportService.importSuppliers(file);
    }
}
