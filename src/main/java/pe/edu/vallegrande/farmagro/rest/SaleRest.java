package pe.edu.vallegrande.farmagro.rest;

import pe.edu.vallegrande.farmagro.dto.SaleDTO;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.util.StatusFilters;
import pe.edu.vallegrande.farmagro.dto.SaleDetailDTO;
import pe.edu.vallegrande.farmagro.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SaleRest {

    private final SaleService saleService;

    @GetMapping
    public Flux<SaleDTO> getAll(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE_VALUE) int size) {
        return saleService.findAll(page, size);
    }

    @GetMapping("/status/{status}")
    public Flux<SaleDTO> getByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE_VALUE) int size) {
        return StatusFilters.validateOrderStatus(status)
                .flatMapMany(normalizedStatus -> saleService.findByStatus(normalizedStatus, page, size));
    }

    @GetMapping("/customer/{mongoIdCustomer}")
    public Flux<SaleDTO> getByCustomer(
            @PathVariable String mongoIdCustomer,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE_VALUE) int size) {
        return saleService.findByCustomer(mongoIdCustomer, page, size);
    }

    @GetMapping("/seller/{mongoIdUser}")
    public Flux<SaleDTO> getByUser(
            @PathVariable String mongoIdUser,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_VALUE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE_VALUE) int size) {
        return saleService.findByUser(mongoIdUser, page, size);
    }

    @GetMapping("/{id}")
    public Mono<SaleDTO> getById(@PathVariable Integer id) {
        return saleService.findById(id);
    }

    @GetMapping("/{id}/details")
    public Flux<SaleDetailDTO> getDetails(@PathVariable Integer id) {
        return saleService.findDetailsBySaleId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SaleDTO> create(@Valid @RequestBody SaleDTO dto) {
        return saleService.create(dto);
    }

    @PutMapping("/{id}")
    public Mono<SaleDTO> update(@PathVariable Integer id,
                                @Valid @RequestBody SaleDTO dto) {
        return saleService.update(id, dto);
    }

    @PatchMapping("/{id}/cancel")
    public Mono<SaleDTO> cancel(@PathVariable Integer id) {
        return saleService.cancel(id);
    }

    @PatchMapping("/{id}/restore")
    public Mono<SaleDTO> restore(@PathVariable Integer id) {
        return saleService.restore(id);
    }

    @PatchMapping("/{id}/pay")
    public Mono<SaleDTO> pay(@PathVariable Integer id) {
        return saleService.pay(id);
    }
}
