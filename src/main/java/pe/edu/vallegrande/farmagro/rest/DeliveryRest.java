package pe.edu.vallegrande.farmagro.rest;

import pe.edu.vallegrande.farmagro.dto.DeliveryDTO;
import pe.edu.vallegrande.farmagro.service.DeliveryService;
import pe.edu.vallegrande.farmagro.util.StatusFilters;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DeliveryRest {

    private static final Map<String, String> CANONICAL_DELIVERY_TYPES = Map.of(
            "DELIVERY", "Delivery",
            "PICKUP", "Pickup",
            "COURIER", "Courier");
    private static final String ALLOWED_TYPES_MESSAGE = "Delivery, Pickup, Courier";

    private final DeliveryService deliveryService;

    @GetMapping
    public Flux<DeliveryDTO> getAll() {
        return deliveryService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<DeliveryDTO> getById(@PathVariable Integer id) {
        return deliveryService.findById(id);
    }

    @GetMapping("/status/{status}")
    public Flux<DeliveryDTO> getByStatus(@PathVariable String status) {
        return StatusFilters.validateDeliveryStatus(status)
                .flatMapMany(deliveryService::findByStatus);
    }

    @GetMapping("/sale/{saleId}")
    public Flux<DeliveryDTO> getBySale(@PathVariable Integer saleId) {
        return deliveryService.findBySale(saleId);
    }

    @GetMapping("/type/{deliveryType}")
    public Flux<DeliveryDTO> getByType(@PathVariable String deliveryType) {
        String canonicalType = CANONICAL_DELIVERY_TYPES.get(deliveryType.toUpperCase());
        if (canonicalType == null) {
            return Flux.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tipo inválido: '" + deliveryType + "'. Valores permitidos: " + ALLOWED_TYPES_MESSAGE));
        }
        return deliveryService.findByType(canonicalType);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<DeliveryDTO> create(@Valid @RequestBody DeliveryDTO dto) {
        return deliveryService.create(dto);
    }

    @PutMapping("/{id}")
    public Mono<DeliveryDTO> update(@PathVariable Integer id,
                                    @Valid @RequestBody DeliveryDTO dto) {
        return deliveryService.update(id, dto);
    }

    @PatchMapping("/{id}/cancel")
    public Mono<DeliveryDTO> cancel(@PathVariable Integer id) {
        return deliveryService.cancel(id);
    }

    @PatchMapping("/{id}/restore")
    public Mono<DeliveryDTO> restore(@PathVariable Integer id) {
        return deliveryService.restore(id);
    }
}
