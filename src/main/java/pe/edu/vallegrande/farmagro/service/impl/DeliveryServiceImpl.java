package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.dto.DeliveryDTO;
import pe.edu.vallegrande.farmagro.model.sql.Delivery;
import pe.edu.vallegrande.farmagro.repository.sql.DeliveryRepository;
import pe.edu.vallegrande.farmagro.repository.sql.SaleRepository;
import pe.edu.vallegrande.farmagro.repository.sql.GeographicLocationRepository;
import pe.edu.vallegrande.farmagro.service.DeliveryService;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final SaleRepository saleRepository;
    private final GeographicLocationRepository geographicLocationRepository;


    @Override
    public Flux<DeliveryDTO> findAll() {
        return deliveryRepository.findAll().map(this::toDTO);
    }

    @Override
    public Mono<DeliveryDTO> findById(Integer id) {
        return deliveryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Entrega no encontrada con id: " + id)))
                .map(this::toDTO);
    }

    @Override
    public Flux<DeliveryDTO> findByStatus(String status) {
        return deliveryRepository.findByStatus(status).map(this::toDTO);
    }

    @Override
    public Flux<DeliveryDTO> findBySale(Integer saleId) {
        return deliveryRepository.findBySaleId(saleId).map(this::toDTO);
    }

    @Override
    public Flux<DeliveryDTO> findByType(String deliveryType) {
        return deliveryRepository.findByDeliveryType(deliveryType).map(this::toDTO);
    }


    @Override
    @Transactional
    public Mono<DeliveryDTO> create(DeliveryDTO dto) {
        return validateDeliveryDates(dto)
                .then(validateInitialStatus(dto.getStatus()))
                .then(validateSaleExists(dto.getSaleId()))
                .then(validateNoActiveDelivery(dto.getSaleId(), null))
                .then(validateGeographicLocationExists(dto.getGeographicLocationId()))
                .then(Mono.fromCallable(() -> toEntity(dto)))
                .flatMap(deliveryRepository::save)
                .map(this::toDTO);
    }


    @Override
    @Transactional
    public Mono<DeliveryDTO> update(Integer id, DeliveryDTO dto) {
        return deliveryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Entrega no encontrada con id: " + id)))
                .flatMap(entity -> validateDeliveryUpdate(entity, dto).thenReturn(entity))
                .flatMap(entity -> updateDelivery(entity, dto))
                .map(this::toDTO);
    }

    private Mono<Void> validateDeliveryUpdate(Delivery delivery, DeliveryDTO dto) {
        if (AppConstants.STATUS_CANCELLED.equals(delivery.getStatus())) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "No se puede editar un delivery anulado"));
        }
        if (AppConstants.STATUS_DELIVERED.equals(delivery.getStatus())) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "No se puede editar una entrega completada"));
        }
        if (dto.getSaleId() != null && !dto.getSaleId().equals(delivery.getSaleId())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Una entrega no puede pasar a otra venta; anúlela y programe una nueva"));
        }
        return validateDeliveryDates(dto)
                .then(validateSaleExists(dto.getSaleId()))
                .then(validateGeographicLocationExists(dto.getGeographicLocationId()))
                .then(validateStatusTransition(delivery, dto.getStatus(), dto.getActualDate()));
    }

    private Mono<Delivery> updateDelivery(Delivery delivery, DeliveryDTO dto) {
        delivery.setDeliveryType(dto.getDeliveryType());
        delivery.setReference(dto.getReference());
        delivery.setDeliveryAddress(dto.getDeliveryAddress());
        delivery.setGeographicLocationId(dto.getGeographicLocationId());
        delivery.setScheduledDate(dto.getScheduledDate());
        delivery.setActualDate(dto.getActualDate());
        delivery.setStatus(dto.getStatus() != null ? dto.getStatus() : delivery.getStatus());
        delivery.setNotes(dto.getNotes());
        delivery.setSaleId(dto.getSaleId());
        delivery.setUpdatedAt(LocalDateTime.now());
        return deliveryRepository.save(delivery);
    }


    @Override
    public Mono<DeliveryDTO> cancel(Integer id) {
        return deliveryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Entrega no encontrada con id: " + id)))
                .flatMap(entity -> {
                    if (AppConstants.STATUS_CANCELLED.equals(entity.getStatus())) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "El delivery ya se encuentra anulado"));
                    }
                    if (AppConstants.STATUS_DELIVERED.equals(entity.getStatus())) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "No se puede cancelar una entrega ya completada"));
                    }
                    entity.setStatus(AppConstants.STATUS_CANCELLED);
                    entity.setDeletedAt(LocalDateTime.now());
                    entity.setRestoredAt(null);
                    entity.setUpdatedAt(LocalDateTime.now());
                    return deliveryRepository.save(entity);
                })
                .map(this::toDTO);
    }


    @Override
    public Mono<DeliveryDTO> restore(Integer id) {
        return deliveryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Entrega no encontrada con id: " + id)))
                .flatMap(entity -> {
                    if (!AppConstants.STATUS_CANCELLED.equals(entity.getStatus())) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Solo se puede restaurar un delivery anulado"));
                    }
                    return validateSaleExists(entity.getSaleId())
                            .then(validateNoActiveDelivery(entity.getSaleId(), entity.getIdDelivery()))
                            .thenReturn(entity);
                })
                .flatMap(entity -> {
                    entity.setStatus(AppConstants.STATUS_PENDING);
                    entity.setRestoredAt(LocalDateTime.now());
                    entity.setDeletedAt(null);
                    entity.setUpdatedAt(LocalDateTime.now());
                    return deliveryRepository.save(entity);
                })
                .map(this::toDTO);
    }


    private Mono<Void> validateSaleExists(Integer saleId) {
        if (saleId == null) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El id de la venta es obligatorio"));
        }
        return saleRepository.findById(saleId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La venta con id " + saleId + " no existe en SQL Server. "
                                + "Debe crear primero la venta antes de registrar la entrega.")))
                .flatMap(sale -> AppConstants.STATUS_CANCELLED.equals(sale.getStatus())
                        ? Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No se puede registrar una entrega para una venta cancelada"))
                        : Mono.empty());
    }

    // A sale is shipped by at most one active (non-cancelled) delivery.
    private Mono<Void> validateNoActiveDelivery(Integer saleId, Integer excludedDeliveryId) {
        return deliveryRepository.findBySaleId(saleId)
                .filter(delivery -> !AppConstants.STATUS_CANCELLED.equals(delivery.getStatus()))
                .filter(delivery -> !delivery.getIdDelivery().equals(excludedDeliveryId))
                .hasElements()
                .flatMap(exists -> exists
                        ? Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "La venta " + saleId + " ya tiene una entrega activa"))
                        : Mono.empty());
    }

    private Mono<Void> validateGeographicLocationExists(Integer geographicLocationId) {
        if (geographicLocationId == null) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El id de geographicLocation es obligatorio"));
        }
        return geographicLocationRepository.existsById(geographicLocationId)
                .flatMap(exists -> exists
                        ? Mono.empty()
                        : Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "El ubigeo " + geographicLocationId + " no existe en el catálogo")));
    }

    private Mono<Void> validateDeliveryDates(DeliveryDTO dto) {
        if (dto.getScheduledDate() == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha programada es obligatoria"));
        }
        // Early deliveries are valid (actualDate before scheduledDate); see DELIVERIES_date_ck in v13.
        if (AppConstants.STATUS_DELIVERED.equals(dto.getStatus()) && dto.getActualDate() == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Una entrega completada debe incluir su fecha real"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateInitialStatus(String status) {
        if (AppConstants.STATUS_CANCELLED.equals(status) || AppConstants.STATUS_DELIVERED.equals(status)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Una entrega debe crearse como PENDING o ON_THE_WAY; use las operaciones de transición para completarla o cancelarla"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateStatusTransition(Delivery current, String newStatus, LocalDateTime actualDate) {
        if (newStatus == null || newStatus.equals(current.getStatus())) {
            return Mono.empty();
        }
        if (AppConstants.STATUS_CANCELLED.equals(newStatus)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use la operación de cancelación para cancelar una entrega"));
        }
        if (AppConstants.STATUS_DELIVERED.equals(newStatus) && actualDate == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Una entrega completada debe incluir su fecha real"));
        }
        if (AppConstants.STATUS_DELIVERED.equals(current.getStatus())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Una entrega completada no puede volver a un estado anterior"));
        }
        return Mono.empty();
    }


    private Delivery toEntity(DeliveryDTO dto) {
        return Delivery.builder()
                .deliveryType(dto.getDeliveryType())
                .reference(dto.getReference())
                .deliveryAddress(dto.getDeliveryAddress())
                .geographicLocationId(dto.getGeographicLocationId())
                .scheduledDate(dto.getScheduledDate())
                .actualDate(dto.getActualDate())
                .status(dto.getStatus() != null ? dto.getStatus() : AppConstants.STATUS_PENDING)
                .notes(dto.getNotes())
                .saleId(dto.getSaleId())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private DeliveryDTO toDTO(Delivery delivery) {
        return DeliveryDTO.builder()
                .idDelivery(delivery.getIdDelivery())
                .deliveryType(delivery.getDeliveryType())
                .reference(delivery.getReference())
                .deliveryAddress(delivery.getDeliveryAddress())
                .geographicLocationId(delivery.getGeographicLocationId())
                .scheduledDate(delivery.getScheduledDate())
                .actualDate(delivery.getActualDate())
                .status(delivery.getStatus())
                .notes(delivery.getNotes())
                .saleId(delivery.getSaleId())
                .createdAt(delivery.getCreatedAt())
                .updatedAt(delivery.getUpdatedAt())
                .deletedAt(delivery.getDeletedAt())
                .restoredAt(delivery.getRestoredAt())
                .build();
    }
}
