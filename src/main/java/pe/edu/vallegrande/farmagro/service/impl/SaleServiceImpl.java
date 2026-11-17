package pe.edu.vallegrande.farmagro.service.impl;

import lombok.extern.slf4j.Slf4j;
import pe.edu.vallegrande.farmagro.dto.DeliveryDTO;
import pe.edu.vallegrande.farmagro.dto.SaleDTO;
import pe.edu.vallegrande.farmagro.dto.SaleDeliveryDTO;
import pe.edu.vallegrande.farmagro.dto.SaleDetailDTO;
import pe.edu.vallegrande.farmagro.model.Person;
import pe.edu.vallegrande.farmagro.model.sql.Delivery;
import pe.edu.vallegrande.farmagro.model.sql.Sale;
import pe.edu.vallegrande.farmagro.model.sql.SaleDetail;
import pe.edu.vallegrande.farmagro.repository.sql.DeliveryRepository;
import pe.edu.vallegrande.farmagro.repository.sql.SaleDetailRepository;
import pe.edu.vallegrande.farmagro.repository.sql.SaleRepository;
import pe.edu.vallegrande.farmagro.service.DeliveryService;
import pe.edu.vallegrande.farmagro.service.SaleService;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.util.PaginationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

    private static final int MAX_INVOICE_ATTEMPTS = 3;
    private static final int HOME_DELIVERY_LEAD_DAYS = 2;
    private static final int PICKUP_LEAD_DAYS = 1;
    private static final LocalTime DEFAULT_DELIVERY_TIME = LocalTime.of(9, 0);
    private static final String WEB_ORDER_REFERENCE = "Pedido web";
    private static final String PICKUP_REFERENCE = "Recojo en tienda";
    private static final int MAX_LOCK_RETRIES = 3;
    private static final Duration LOCK_RETRY_BACKOFF = Duration.ofMillis(50);

    private final SaleRepository saleRepository;
    private final SaleDetailRepository saleDetailRepository;
    private final SaleMapper saleMapper;
    private final SaleTransitionService saleTransitionService;
    private final SaleDetailValidator saleDetailValidator;
    private final SalePartyValidator salePartyValidator;
    private final SaleFinancialValidator saleFinancialValidator;
    private final DeliveryRepository deliveryRepository;
    private final InvoiceNumberAssigner invoiceNumberAssigner;
    private final TransactionalOperator transactionalOperator;
    private final DeliveryService deliveryService;


    @Override
    public Flux<SaleDTO> findAll() {
        return saleRepository.findAllPaged(AppConstants.DEFAULT_PAGE, AppConstants.DEFAULT_QUERY_LIMIT)
                .flatMap(this::toDTOWithDetails);
    }

    @Override
    public Flux<SaleDTO> findAll(int page, int size) {
        int offset = PaginationValidator.offset(page, size);
        return saleRepository.findAllPaged(offset, size)
                .flatMap(this::toDTOWithDetails);
    }

    public Flux<SaleDTO> findByStatus(String status) {
        return saleRepository.findByStatus(status)
                .flatMap(this::toDTOWithDetails);
    }

    @Override
    public Flux<SaleDTO> findByStatus(String status, int page, int size) {
        int offset = PaginationValidator.offset(page, size);
        return saleRepository.findByStatusPaged(status, offset, size)
                .flatMap(this::toDTOWithDetails);
    }

    public Flux<SaleDTO> findByCustomer(String mongoIdCustomer) {
        return saleRepository.findByMongoIdCustomer(mongoIdCustomer)
                .flatMap(this::toDTOWithDetails);
    }

    @Override
    public Flux<SaleDTO> findByCustomer(String mongoIdCustomer, int page, int size) {
        int offset = PaginationValidator.offset(page, size);
        return saleRepository.findByCustomerPaged(mongoIdCustomer, offset, size)
                .flatMap(this::toDTOWithDetails);
    }

    public Flux<SaleDTO> findByUser(String mongoIdUser) {
        return saleRepository.findByMongoIdUser(mongoIdUser)
                .flatMap(this::toDTOWithDetails);
    }

    @Override
    public Flux<SaleDTO> findByUser(String mongoIdUser, int page, int size) {
        int offset = PaginationValidator.offset(page, size);
        return saleRepository.findByUserPaged(mongoIdUser, offset, size)
                .flatMap(this::toDTOWithDetails);
    }

    @Override
    public Mono<SaleDTO> findById(Integer id) {
        return saleRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Venta no encontrada con id: " + id)))
                .flatMap(this::toDTOWithDetails);
    }

    @Override
    public Flux<SaleDetailDTO> findDetailsBySaleId(Integer saleId) {
        return saleDetailRepository.findBySaleId(saleId)
                .map(saleMapper::toDetailDTO);
    }


    @Override
    @Transactional
    public Mono<SaleDTO> update(Integer id, SaleDTO dto) {
        return saleRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Venta no encontrada con id: " + id)))
                .flatMap(entity -> validateSaleUpdatePreconditions(entity, dto, id))
                .flatMap(entity -> updateSaleFields(entity, dto))
                .flatMap(this::updateSaleDetailsIfProvided);
    }

    private Mono<Sale> validateSaleUpdatePreconditions(Sale entity, SaleDTO dto, Integer saleId) {
        return validateSaleNotCancelled(entity)
                .then(validateSaleNotPaid(entity))
                .then(validateDetailsIfProvided(dto))
                .then(Mono.defer(() -> validateUnchangedSaleDetails(dto, saleId)))
                .then(Mono.fromRunnable(() -> keepStoredInvoiceNumber(entity, dto)))
                .then(Mono.defer(() -> validateUniqueInvoice(dto.getInvoiceNumber(), saleId)))
                .then(Mono.defer(() -> salePartyValidator.validateChangedParties(entity, dto)))
                .flatMap(customer -> {
                    String voucherType = salePartyValidator.determineAndValidateVoucherType(customer, dto.getVoucherType());
                    dto.setVoucherType(voucherType);
                    return invoiceNumberAssigner.validateSeries(dto.getInvoiceNumber(), voucherType).thenReturn(entity);
                });
    }

    // The voucher number is fixed once issued; an update that omits it keeps the stored one.
    private void keepStoredInvoiceNumber(Sale entity, SaleDTO dto) {
        if (invoiceNumberAssigner.needsNumber(dto.getInvoiceNumber())) {
            dto.setInvoiceNumber(entity.getInvoiceNumber());
        }
    }

    private Mono<Void> validateSaleNotCancelled(Sale entity) {
        if (AppConstants.STATUS_CANCELLED.equals(entity.getStatus())) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "No se puede editar una venta anulada"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateSaleNotPaid(Sale entity) {
        if (AppConstants.STATUS_PAID.equals(entity.getStatus())) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "No se puede editar una venta que ya ha sido pagada"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateDetailsIfProvided(SaleDTO dto) {
        if (dto.getDetails() == null || dto.getDetails().isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La venta debe incluir al menos un detalle de producto"));
        }
        return saleFinancialValidator.validate(dto);
    }

    private Mono<Void> validateUnchangedSaleDetails(SaleDTO dto, Integer saleId) {
        return DetailSignatureSupport.validateUnchanged(
                saleDetailRepository.findBySaleId(saleId),
                dto.getDetails().stream().map(this::saleDetailKey).toList(),
                this::saleDetailKey,
                "Los detalles de una venta existente no se pueden modificar; registre una venta nueva");
    }

    private String saleDetailKey(SaleDetail detail) {
        return DetailSignatureSupport.compose(new DetailSignatureSupport.DetailSignature(catalogItemRef(detail),
                detail.getQuantity(), detail.getUnitPrice(), detail.getItemDiscount()));
    }

    private String saleDetailKey(SaleDetailDTO detail) {
        BigDecimal itemDiscount = detail.getItemDiscount() == null ? BigDecimal.ZERO : detail.getItemDiscount();
        return DetailSignatureSupport.compose(new DetailSignatureSupport.DetailSignature(catalogItemRef(detail),
                detail.getQuantity(), detail.getUnitPrice(), itemDiscount));
    }

    private Mono<Sale> updateSaleFields(Sale entity, SaleDTO dto) {
        saleMapper.applyUpdateFields(entity, dto);
        return saleRepository.save(entity);
    }

    private Mono<SaleDTO> updateSaleDetailsIfProvided(Sale savedSale) {
        return toDTOWithDetails(savedSale);
    }


    // When SQL Server picks this transaction as a deadlock victim it rolls it back entirely; the
    // inventory change in Mongo is the last step, so nothing else needs undoing and the whole sale
    // is retried in a new transaction (with the voucher number the client sent, if any).
    @Override
    public Mono<SaleDTO> create(SaleDTO dto) {
        String requestedInvoiceNumber = dto.getInvoiceNumber();
        return Mono.defer(() -> {
                    dto.setInvoiceNumber(requestedInvoiceNumber);
                    return transactionalOperator.transactional(createInTransaction(dto));
                })
                .retryWhen(Retry.backoff(MAX_LOCK_RETRIES, LOCK_RETRY_BACKOFF)
                        .filter(PessimisticLockingFailureException.class::isInstance)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }

    private Mono<SaleDTO> createInTransaction(SaleDTO dto) {
        return validateSaleStatus(dto)
                .then(performSaleValidations(dto))
                .then(salePartyValidator.validateCustomerAndSeller(dto))
                .flatMap(tuple -> createSaleEntity(dto, tuple))
                .flatMap(savedSale -> createSaleDetailsAndAdjustStock(savedSale, dto));
    }

    private Mono<Void> validateSaleStatus(SaleDTO dto) {
        if (dto.getStatus() != null && AppConstants.STATUS_CANCELLED.equalsIgnoreCase(dto.getStatus())) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "No se puede registrar una venta directamente con estado CANCELLED"));
        }
        return Mono.empty();
    }

    private Mono<Void> performSaleValidations(SaleDTO dto) {
        Mono<Void> uniqueInvoice = invoiceNumberAssigner.needsNumber(dto.getInvoiceNumber())
                ? Mono.empty() : validateUniqueInvoice(dto.getInvoiceNumber(), null);
        return uniqueInvoice
                .then(saleDetailValidator.validate(dto.getDetails()))
                .then(saleFinancialValidator.validate(dto));
    }

    private Mono<Sale> createSaleEntity(SaleDTO dto, Tuple2<Person, Person> tuple) {
        Person customer = tuple.getT1();
        String voucherType = salePartyValidator.determineAndValidateVoucherType(customer, dto.getVoucherType());
        dto.setVoucherType(voucherType);
        if (!invoiceNumberAssigner.needsNumber(dto.getInvoiceNumber())) {
            return invoiceNumberAssigner.validateSeries(dto.getInvoiceNumber(), voucherType)
                    .then(Mono.defer(() -> saleRepository.save(saleMapper.toEntity(dto))));
        }
        return saveWithNextInvoiceNumber(dto, 1);
    }

    // Two concurrent sales may read the same last correlative; the UNIQUE constraint rejects the
    // second insert, which then retries with the next number.
    private Mono<Sale> saveWithNextInvoiceNumber(SaleDTO dto, int attempt) {
        return invoiceNumberAssigner.nextNumber(dto.getVoucherType())
                .flatMap(invoiceNumber -> {
                    dto.setInvoiceNumber(invoiceNumber);
                    return saleRepository.save(saleMapper.toEntity(dto));
                })
                .onErrorResume(error -> error instanceof DataIntegrityViolationException && attempt < MAX_INVOICE_ATTEMPTS,
                        error -> saveWithNextInvoiceNumber(dto, attempt + 1));
    }

    // SQL work first (details, then the requested delivery) and the Mongo stock change last, so a
    // failure anywhere before it rolls everything back without inventory to undo.
    private Mono<SaleDTO> createSaleDetailsAndAdjustStock(Sale savedSale, SaleDTO dto) {
        return Flux.fromIterable(dto.getDetails())
                .map(detailDTO -> saleMapper.toDetailEntity(detailDTO, savedSale.getIdSale()))
                .concatMap(saleDetailRepository::save)
                .collectList()
                .flatMap(details -> createRequestedDelivery(savedSale, dto.getDelivery()).thenReturn(details))
                .flatMap(details -> saleTransitionService.adjustStockForNewSale(dto.getDetails())
                        .then(Mono.fromCallable(() -> saleMapper.toDTO(savedSale,
                                details.stream().map(saleMapper::toDetailDTO).toList()))));
    }

    private Mono<Void> createRequestedDelivery(Sale sale, SaleDeliveryDTO requested) {
        if (requested == null) return Mono.empty();
        boolean homeDelivery = AppConstants.DELIVERY_TYPE_DELIVERY.equals(requested.getDeliveryType());
        if (homeDelivery && (requested.getDeliveryAddress() == null || requested.getDeliveryAddress().isBlank())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La entrega a domicilio requiere la dirección de entrega"));
        }
        LocalDateTime scheduled = requested.getScheduledDate() != null ? requested.getScheduledDate()
                : LocalDate.now(AppConstants.BUSINESS_ZONE)
                        .plusDays(homeDelivery ? HOME_DELIVERY_LEAD_DAYS : PICKUP_LEAD_DAYS)
                        .atTime(DEFAULT_DELIVERY_TIME);
        String reference = requested.getReference() != null && !requested.getReference().isBlank()
                ? requested.getReference().trim() : homeDelivery ? WEB_ORDER_REFERENCE : PICKUP_REFERENCE;
        DeliveryDTO delivery = DeliveryDTO.builder()
                .deliveryType(requested.getDeliveryType())
                .deliveryAddress(homeDelivery ? requested.getDeliveryAddress().trim() : null)
                .reference(reference)
                .geographicLocationId(requested.getGeographicLocationId())
                .scheduledDate(scheduled)
                .status(AppConstants.STATUS_PENDING)
                .saleId(sale.getIdSale())
                .build();
        return deliveryService.create(delivery).then();
    }

    @Override
    @Transactional
    public Mono<SaleDTO> cancel(Integer id) {
        return findSaleOrNotFound(id)
                .flatMap(this::cancelSale)
                .flatMap(this::toDTOWithDetails);
    }

    // A delivered sale cannot be cancelled (its goods already left); open deliveries are cancelled
    // together with the sale in the same SQL transaction.
    private Mono<Sale> cancelSale(Sale sale) {
        return validateCanCancel(sale)
                .then(deliveryRepository.findBySaleId(sale.getIdSale()).collectList())
                .flatMap(deliveries -> validateNotDelivered(deliveries)
                        .then(loadSaleDetails(sale))
                        .flatMap(details -> saleTransitionService.cancel(sale, details))
                        .flatMap(cancelledSale -> cancelOpenDeliveries(deliveries).thenReturn(cancelledSale)));
    }

    private Mono<Void> validateNotDelivered(List<Delivery> deliveries) {
        boolean delivered = deliveries.stream().anyMatch(delivery -> AppConstants.STATUS_DELIVERED.equals(delivery.getStatus()));
        return delivered
                ? Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No se puede anular una venta que ya fue entregada"))
                : Mono.empty();
    }

    private Mono<Void> cancelOpenDeliveries(List<Delivery> deliveries) {
        LocalDateTime now = LocalDateTime.now();
        return Flux.fromIterable(deliveries)
                .filter(delivery -> !AppConstants.STATUS_CANCELLED.equals(delivery.getStatus()))
                .doOnNext(delivery -> {
                    delivery.setStatus(AppConstants.STATUS_CANCELLED);
                    delivery.setDeletedAt(now);
                    delivery.setRestoredAt(null);
                    delivery.setUpdatedAt(now);
                })
                .concatMap(deliveryRepository::save)
                .then();
    }

    private Mono<Void> validateCanCancel(Sale sale) {
        if (AppConstants.STATUS_CANCELLED.equals(sale.getStatus())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "La venta ya se encuentra anulada"));
        }
        return Mono.empty();
    }


    @Override
    @Transactional
    public Mono<SaleDTO> restore(Integer id) {
        return findSaleOrNotFound(id)
                .flatMap(this::restoreSale)
                .flatMap(this::toDTOWithDetails);
    }

    private Mono<Sale> restoreSale(Sale sale) {
        return validateCanRestore(sale)
                .then(loadSaleDetails(sale))
                .flatMap(details -> saleTransitionService.restore(sale, details));
    }

    private Mono<Void> validateCanRestore(Sale sale) {
        if (!AppConstants.STATUS_CANCELLED.equals(sale.getStatus())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se puede restaurar una venta anulada"));
        }
        return Mono.empty();
    }

    private Mono<Sale> findSaleOrNotFound(Integer id) {
        return saleRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Venta no encontrada con id: " + id)));
    }

    private Mono<List<SaleDetailDTO>> loadSaleDetails(Sale sale) {
        return saleDetailRepository.findBySaleId(sale.getIdSale())
                .map(saleMapper::toDetailDTO)
                .collectList();
    }


    @Override
    public Mono<SaleDTO> pay(Integer id) {
        return saleRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Venta no encontrada con id: " + id)))
                .flatMap(entity -> {
                    if (AppConstants.STATUS_PAID.equals(entity.getStatus())) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "La venta ya se encuentra pagada"));
                    }
                    if (AppConstants.STATUS_CANCELLED.equals(entity.getStatus())) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "No se puede pagar una venta anulada"));
                    }
                    entity.setStatus(AppConstants.STATUS_PAID);
                    entity.setUpdatedAt(LocalDateTime.now());
                    return saleRepository.save(entity);
                })
                .flatMap(this::toDTOWithDetails);
    }


    private Mono<Void> validateUniqueInvoice(String invoiceNumber, Integer excludeId) {
        Mono<Boolean> recordExists = (excludeId == null)
                ? saleRepository.existsByInvoiceNumber(invoiceNumber)
                : saleRepository.existsByInvoiceNumberAndIdSaleNot(invoiceNumber, excludeId);

        return recordExists.flatMap(exists -> exists
                ? Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Ya existe una venta con el comprobante " + invoiceNumber))
                : Mono.empty());
    }

    private Mono<SaleDTO> toDTOWithDetails(Sale sale) {
        return saleDetailRepository.findBySaleId(sale.getIdSale())
                .map(saleMapper::toDetailDTO)
                .collectList()
                .map(details -> saleMapper.toDTO(sale, details));
    }

    private DetailSignatureSupport.CatalogItemRef catalogItemRef(SaleDetail detail) {
        return new DetailSignatureSupport.CatalogItemRef(
                detail.getMongoIdProduct(), detail.getMongoIdPresentation(), detail.getMongoIdBatch());
    }

    private DetailSignatureSupport.CatalogItemRef catalogItemRef(SaleDetailDTO detail) {
        return new DetailSignatureSupport.CatalogItemRef(
                detail.getMongoIdProduct(), detail.getMongoIdPresentation(), detail.getMongoIdBatch());
    }
}
