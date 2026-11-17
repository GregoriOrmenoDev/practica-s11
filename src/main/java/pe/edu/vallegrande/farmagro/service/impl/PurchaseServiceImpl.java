package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.dto.PurchaseDTO;
import pe.edu.vallegrande.farmagro.dto.PurchaseDetailDTO;
import pe.edu.vallegrande.farmagro.model.sql.Purchase;
import pe.edu.vallegrande.farmagro.model.sql.PurchaseDetail;
import pe.edu.vallegrande.farmagro.repository.sql.PurchaseDetailRepository;
import pe.edu.vallegrande.farmagro.repository.sql.PurchaseRepository;
import pe.edu.vallegrande.farmagro.service.PurchaseService;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.util.PaginationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private static final int STOCK_DECREASE = -1;
    private static final int STOCK_INCREASE = 1;

    private final PurchaseRepository purchaseRepository;
    private final PurchaseDetailRepository purchaseDetailRepository;
    private final PurchaseInventoryService purchaseInventoryService;
    private final PurchaseDetailValidator purchaseDetailValidator;
    private final PurchasePartyValidator purchasePartyValidator;
    private final PurchaseFinancialValidator purchaseFinancialValidator;
    private final PurchaseMapper purchaseMapper;
    private final PurchaseBatchService purchaseBatchService;

    @Override
    public Flux<PurchaseDTO> findAll(int page, int size) {
        return purchaseRepository.findAllPaged(PaginationValidator.offset(page, size), size)
                .flatMapSequential(this::toDTOWithDetails);
    }

    @Override
    public Flux<PurchaseDTO> findByStatus(String status, int page, int size) {
        return purchaseRepository.findByStatusPaged(status, PaginationValidator.offset(page, size), size)
                .flatMapSequential(this::toDTOWithDetails);
    }

    @Override
    public Flux<PurchaseDTO> findBySupplier(String mongoIdSupplier) {
        return purchaseRepository.findByMongoIdSupplier(mongoIdSupplier)
                .flatMap(this::toDTOWithDetails);
    }

    @Override
    public Mono<PurchaseDTO> findById(Integer id) {
        return purchaseRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Compra no encontrada con id: " + id)))
                .flatMap(this::toDTOWithDetails);
    }

    @Override
    @Transactional
    public Mono<PurchaseDTO> create(PurchaseDTO dto) {
        return validateInitialStatus(dto.getStatus())
                .then(validateDeliveryDate(dto))
                .then(purchasePartyValidator.validate(dto.getMongoIdSupplier(), dto.getMongoIdUser()))
                .then(purchaseDetailValidator.validate(dto.getDetails()))
                .then(purchaseFinancialValidator.validate(dto))
                .then(validateUniqueInvoice(dto.getInvoiceNumber(), null))
                .then(purchaseBatchService.createNewBatches(dto.getDetails()))
                .flatMap(createdBatches -> saveCreatedPurchase(dto)
                        .onErrorResume(error -> purchaseBatchService.removeBatches(createdBatches).then(Mono.error(error))));
    }

    private Mono<PurchaseDTO> saveCreatedPurchase(PurchaseDTO dto) {
        return Mono.fromCallable(() -> purchaseMapper.toEntity(dto))
                .flatMap(purchaseRepository::save)
                .flatMap(saved -> saveDetails(saved.getIdPurchase(), dto.getDetails())
                        .then(purchaseInventoryService.adjustForCreatedPurchase(saved, dto.getDetails()))
                        .then(toDTOWithDetails(saved)));
    }

    @Override
    @Transactional
    public Mono<PurchaseDTO> update(Integer id, PurchaseDTO dto) {
        return purchaseRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Compra no encontrada con id: " + id)))
                .flatMap(entity -> validatePurchaseUpdate(dto, id, entity))
                .flatMap(entity -> applyUpdate(entity, dto))
                .flatMap(this::toDTOWithDetails);
    }

    private Mono<Void> validateInitialStatus(String status) {
        if (AppConstants.STATUS_CANCELLED.equals(status)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Una compra se registra como PENDING (pedida) o PAID (recibida); para anularla use la operación de anulación"));
        }
        return Mono.empty();
    }

    // Lifecycle: PENDING (ordered) -> PAID (received, adds stock). CANCELLED only through delete/restore.
    private Mono<Void> validateStatusTransition(Purchase entity, String requestedStatus) {
        if (isCancelledPurchase(entity)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede editar una compra anulada; restáurela primero"));
        }
        String targetStatus = requestedStatus != null ? requestedStatus : entity.getStatus();
        if (targetStatus.equals(entity.getStatus()) || isReception(entity.getStatus(), targetStatus)) {
            return Mono.empty();
        }
        if (AppConstants.STATUS_CANCELLED.equals(targetStatus)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use la operación de anulación para anular una compra"));
        }
        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Una compra recibida (PAID) no puede volver a PENDING"));
    }

    private boolean isReception(String currentStatus, String targetStatus) {
        return AppConstants.STATUS_PENDING.equals(currentStatus) && AppConstants.STATUS_PAID.equals(targetStatus);
    }

    // Confirming the reception adds the purchased units; SQL failure reverts the stock change.
    private Mono<Purchase> applyUpdate(Purchase entity, PurchaseDTO dto) {
        if (dto.getStatus() == null || !isReception(entity.getStatus(), dto.getStatus())) {
            return updatePurchaseFields(entity, dto);
        }
        return purchaseDetailRepository.findByPurchasesIdPurchase(entity.getIdPurchase())
                .collectList()
                .flatMap(details -> purchaseInventoryService.adjustPurchaseDetailsWithRollback(details, STOCK_INCREASE)
                        .then(Mono.defer(() -> updatePurchaseFields(entity, dto)
                                .onErrorResume(error -> purchaseInventoryService
                                        .adjustPurchaseDetailsWithRollback(details, STOCK_DECREASE)
                                        .onErrorResume(rollbackError -> {
                                            log.error("Failed to revert received stock after purchase update failure", rollbackError);
                                            return Mono.empty();
                                        })
                                        .then(Mono.error(error))))));
    }

    private Mono<Purchase> validatePurchaseUpdate(PurchaseDTO dto, Integer id, Purchase entity) {
        // Details are immutable, so the catalog is not checked again: a product or batch deactivated
        // after the order must not block confirming its reception.
        return validateStatusTransition(entity, dto.getStatus())
                .then(validateDeliveryDate(dto))
                .then(purchaseDetailValidator.requireDetails(dto.getDetails()))
                .then(Mono.defer(() -> purchaseFinancialValidator.validate(dto)))
                .then(Mono.defer(() -> validateUnchangedPurchaseDetails(dto, id)))
                .then(validateUniqueInvoice(dto.getInvoiceNumber(), id))
                .then(purchasePartyValidator.validateChanged(entity, dto.getMongoIdSupplier(), dto.getMongoIdUser()))
                .thenReturn(entity);
    }

    private Mono<Void> validateUnchangedPurchaseDetails(PurchaseDTO dto, Integer purchaseId) {
        return DetailSignatureSupport.validateUnchanged(
                purchaseDetailRepository.findByPurchasesIdPurchase(purchaseId),
                dto.getDetails().stream().map(this::purchaseDetailKey).toList(),
                this::purchaseDetailKey,
                "Los detalles de una compra existente no se pueden modificar; registre una compra nueva");
    }

    private String purchaseDetailKey(PurchaseDetail detail) {
        return DetailSignatureSupport.compose(new DetailSignatureSupport.DetailSignature(catalogItemRef(detail),
                detail.getQuantity(), detail.getUnitPrice(), detail.getSubtotal()));
    }

    private String purchaseDetailKey(PurchaseDetailDTO detail) {
        return DetailSignatureSupport.compose(new DetailSignatureSupport.DetailSignature(catalogItemRef(detail),
                detail.getQuantity(), detail.getUnitPrice(), detail.getSubtotal()));
    }

    private Mono<Purchase> updatePurchaseFields(Purchase entity, PurchaseDTO dto) {
        entity.setInvoiceNumber(dto.getInvoiceNumber());
        entity.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        entity.setSubtotal(dto.getSubtotal());
        entity.setIgv(dto.getIgv());
        entity.setTotal(dto.getTotal());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        entity.setNotes(dto.getNotes());
        entity.setMongoIdSupplier(dto.getMongoIdSupplier());
        entity.setMongoIdUser(dto.getMongoIdUser());
        entity.setUpdatedAt(LocalDateTime.now());
        return purchaseRepository.save(entity);
    }

    @Override
    @Transactional
    public Mono<PurchaseDTO> delete(Integer id) {
        return purchaseRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Compra no encontrada con id: " + id)))
                .flatMap(this::deletePurchase)
                .flatMap(this::toDTOWithDetails);
    }

    private Mono<Purchase> deletePurchase(Purchase purchase) {
        if (isCancelledPurchase(purchase)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La compra ya se encuentra anulada"));
        }
        return purchaseDetailRepository.findByPurchasesIdPurchase(purchase.getIdPurchase())
                .collectList()
                .flatMap(details -> deletePurchaseWithDetails(purchase, details));
    }

    // Cancelling (logical delete) removes the stock only if it had entered (PAID).
    private Mono<Purchase> deletePurchaseWithDetails(Purchase purchase, List<PurchaseDetail> details) {
        boolean stockReceived = isReceivedPurchase(purchase);
        Mono<Void> inventoryChange = stockReceived
                ? purchaseInventoryService.adjustPurchaseDetailsWithRollback(details, STOCK_DECREASE) : Mono.empty();
        return inventoryChange.then(Mono.defer(() -> persistDeletedPurchase(purchase, details, stockReceived)));
    }

    private Mono<Purchase> persistDeletedPurchase(Purchase purchase, List<PurchaseDetail> details, boolean stockReceived) {
        LocalDateTime now = LocalDateTime.now();
        purchase.setStatus(AppConstants.STATUS_CANCELLED);
        purchase.setDeletedAt(now);
        purchase.setRestoredAt(null);
        purchase.setUpdatedAt(now);
        return purchaseRepository.save(purchase)
                .onErrorResume(error -> compensateFailedDelete(details, stockReceived, error));
    }

    private Mono<Purchase> compensateFailedDelete(List<PurchaseDetail> details, boolean stockReceived,
                                                   Throwable originalError) {
        if (!stockReceived) {
            return Mono.error(originalError);
        }
        return purchaseInventoryService.adjustPurchaseDetailsWithRollback(details, STOCK_INCREASE)
                .onErrorResume(rollbackError -> {
                    log.error("Failed to restore purchase inventory after SQL update failure", rollbackError);
                    return Mono.empty();
                })
                .then(Mono.error(originalError));
    }

    @Override
    @Transactional
    public Mono<PurchaseDTO> restore(Integer id) {
        return purchaseRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Compra no encontrada con id: " + id)))
                .flatMap(this::restorePurchase)
                .flatMap(this::toDTOWithDetails);
    }

    // A restored purchase goes back to PENDING (ordered, no stock); confirming it again adds the stock.
    private Mono<Purchase> restorePurchase(Purchase purchase) {
        if (!isCancelledPurchase(purchase)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La compra no está anulada"));
        }
        LocalDateTime now = LocalDateTime.now();
        purchase.setStatus(AppConstants.STATUS_PENDING);
        purchase.setRestoredAt(now);
        purchase.setDeletedAt(null);
        purchase.setUpdatedAt(now);
        return purchaseRepository.save(purchase);
    }

    // Older rows may be logically deleted (deleted_at) without the CANCELLED status.
    private boolean isCancelledPurchase(Purchase purchase) {
        return AppConstants.STATUS_CANCELLED.equals(purchase.getStatus()) || purchase.getDeletedAt() != null;
    }

    private boolean isReceivedPurchase(Purchase purchase) {
        return AppConstants.STATUS_PAID.equals(purchase.getStatus());
    }

    private Mono<Void> validateDeliveryDate(PurchaseDTO dto) {
        if (dto.getExpectedDeliveryDate() == null) {
            return Mono.empty();
        }
        LocalDateTime purchaseDate = dto.getPurchaseDate() != null ? dto.getPurchaseDate() : LocalDateTime.now();
        if (dto.getExpectedDeliveryDate().isBefore(purchaseDate)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha de entrega prevista no puede ser anterior a la fecha de compra"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateUniqueInvoice(String invoiceNumber, Integer excludeId) {
        Mono<Boolean> recordExists = (excludeId == null)
                ? purchaseRepository.existsByInvoiceNumber(invoiceNumber)
                : purchaseRepository.existsByInvoiceNumberAndIdPurchaseNot(invoiceNumber, excludeId);

        return recordExists.flatMap(exists -> exists
                ? Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Ya existe una compra con el comprobante " + invoiceNumber))
                : Mono.empty());
    }

    private Mono<Void> saveDetails(Integer purchaseId, List<PurchaseDetailDTO> details) {
        List<PurchaseDetail> entities = details.stream()
                .map(detail -> PurchaseDetail.builder()
                        .quantity(detail.getQuantity())
                        .unitPrice(detail.getUnitPrice())
                        .subtotal(detail.getSubtotal())
                        .purchasesIdPurchase(purchaseId)
                        .mongoIdProduct(detail.getMongoIdProduct())
                        .mongoIdPresentation(detail.getMongoIdPresentation())
                        .mongoIdBatch(detail.getMongoIdBatch())
                        .build())
                .toList();
        return purchaseDetailRepository.saveAll(entities).then();
    }

    private Mono<PurchaseDTO> toDTOWithDetails(Purchase purchase) {
        return purchaseDetailRepository.findByPurchasesIdPurchase(purchase.getIdPurchase())
                .map(purchaseMapper::detailToDTO)
                .collectList()
                .map(details -> purchaseMapper.toDTO(purchase, details));
    }

    private DetailSignatureSupport.CatalogItemRef catalogItemRef(PurchaseDetail detail) {
        return new DetailSignatureSupport.CatalogItemRef(
                detail.getMongoIdProduct(), detail.getMongoIdPresentation(), detail.getMongoIdBatch());
    }

    private DetailSignatureSupport.CatalogItemRef catalogItemRef(PurchaseDetailDTO detail) {
        return new DetailSignatureSupport.CatalogItemRef(
                detail.getMongoIdProduct(), detail.getMongoIdPresentation(), detail.getMongoIdBatch());
    }
}
