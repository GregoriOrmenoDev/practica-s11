package pe.edu.vallegrande.farmagro.rest;

import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.dto.PurchaseDTO;
import pe.edu.vallegrande.farmagro.service.PurchaseService;
import pe.edu.vallegrande.farmagro.util.StatusFilters;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PurchaseRest {

    private final PurchaseService purchaseService;

    @GetMapping
    public Flux<PurchaseDTO> getAll(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_LIST_PAGE_SIZE_VALUE) int size) {
        return purchaseService.findAll(page, size);
    }

    @GetMapping("/status/{status}")
    public Flux<PurchaseDTO> getByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_LIST_PAGE_SIZE_VALUE) int size) {
        return StatusFilters.validateOrderStatus(status)
                .flatMapMany(normalizedStatus -> purchaseService.findByStatus(normalizedStatus, page, size));
    }

    @GetMapping("/supplier/{mongoIdSupplier}")
    public Flux<PurchaseDTO> getBySupplier(@PathVariable String mongoIdSupplier) {
        return purchaseService.findBySupplier(mongoIdSupplier);
    }

    @GetMapping("/{id}")
    public Mono<PurchaseDTO> getById(@PathVariable Integer id) {
        return purchaseService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PurchaseDTO> create(@Valid @RequestBody PurchaseDTO dto) {
        return purchaseService.create(dto);
    }

    @PutMapping("/{id}")
    public Mono<PurchaseDTO> update(@PathVariable Integer id,
                                    @Valid @RequestBody PurchaseDTO dto) {
        return purchaseService.update(id, dto);
    }

    @PatchMapping("/{id}/delete")
    public Mono<PurchaseDTO> delete(@PathVariable Integer id) {
        return purchaseService.delete(id);
    }

    @PatchMapping("/{id}/restore")
    public Mono<PurchaseDTO> restore(@PathVariable Integer id) {
        return purchaseService.restore(id);
    }
}
