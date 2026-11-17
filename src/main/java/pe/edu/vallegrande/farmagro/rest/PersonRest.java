package pe.edu.vallegrande.farmagro.rest;

import pe.edu.vallegrande.farmagro.dto.ImportResultDTO;
import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.model.PersonRole;
import pe.edu.vallegrande.farmagro.service.PersonImportService;
import pe.edu.vallegrande.farmagro.service.PersonExportService;
import pe.edu.vallegrande.farmagro.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PersonRest {

    private final PersonService personService;
    private final PersonImportService personImportService;
    private final PersonExportService personExportService;

    @GetMapping
    public Flux<PersonDTO> getAll(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE_VALUE) int size) {
        return personService.findAll(page, size);
    }

    @GetMapping("/status")
    public Flux<PersonDTO> getByStatus(
            @RequestParam Boolean active,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE_VALUE) int size) {
        return personService.findByStatus(active, page, size);
    }

    @GetMapping("/role/{role}")
    public Flux<PersonDTO> getByRole(
            @PathVariable("role") String role,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE_VALUE) int size) {
        String storedRole = toStoredRole(role);
        if (storedRole == null) {
            return Flux.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rol inválido: '" + role + "'. Valores permitidos: ADMINISTRATOR, VENDOR, WAREHOUSE, DELIVERY, CUSTOMER"));
        }
        return personService.findByRole(storedRole, page, size);
    }

    @GetMapping("/role/{role}/status")
    public Flux<PersonDTO> getByRoleAndStatus(
            @PathVariable("role") String role,
            @RequestParam Boolean active,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE_VALUE) int size) {
        String storedRole = toStoredRole(role);
        if (storedRole == null) {
            return Flux.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rol inválido: '" + role + "'. Valores permitidos: ADMINISTRATOR, VENDOR, WAREHOUSE, DELIVERY, CUSTOMER"));
        }
        return personService.findByRoleAndStatus(storedRole, active, page, size);
    }

    private String toStoredRole(String role) {
        try {
            return PersonRole.fromValue(role).toStorageValue();
        } catch (IllegalArgumentException | NullPointerException exception) {
            return null;
        }
    }

    @GetMapping("/{id}")
    public Mono<PersonDTO> getById(@PathVariable String id) {
        return personService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PersonDTO> create(@Valid @RequestBody PersonDTO dto) {
        return personService.create(dto);
    }

    @PutMapping("/{id}")
    public Mono<PersonDTO> update(@PathVariable String id,
                                   @Valid @RequestBody PersonDTO dto) {
        return personService.update(id, dto);
    }

    @PatchMapping("/{id}/delete")
    public Mono<PersonDTO> delete(@PathVariable String id) {
        return personService.delete(id);
    }

    @PatchMapping("/{id}/restore")
    public Mono<PersonDTO> restore(@PathVariable String id) {
        return personService.restore(id);
    }

    @GetMapping("/export/excel")
    public Mono<ResponseEntity<byte[]>> exportExcel() {
        return personExportService.exportExcel()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"persons_farmagro.xlsx\"")
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(bytes));
    }

    @GetMapping("/export/pdf")
    public Mono<ResponseEntity<byte[]>> exportPdf() {
        return personExportService.exportPdf()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"persons_farmagro.pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(bytes));
    }

    @GetMapping("/template/excel")
    public Mono<ResponseEntity<byte[]>> getTemplate() {
        return personExportService.getExcelTemplate()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"persons_template_farmagro.xlsx\"")
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(bytes));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ImportResultDTO> importPersons(@RequestPart("file") FilePart file) {
        return personImportService.importPersons(file);
    }
}
