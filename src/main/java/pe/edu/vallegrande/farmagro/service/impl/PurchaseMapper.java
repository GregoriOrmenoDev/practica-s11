package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.dto.PurchaseDTO;
import pe.edu.vallegrande.farmagro.dto.PurchaseDetailDTO;
import pe.edu.vallegrande.farmagro.model.sql.Purchase;
import pe.edu.vallegrande.farmagro.model.sql.PurchaseDetail;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
final class PurchaseMapper {

    PurchaseDTO toDTO(Purchase purchase, List<PurchaseDetailDTO> details) {
        return PurchaseDTO.builder()
                .idPurchase(purchase.getIdPurchase())
                .purchaseDate(purchase.getPurchaseDate())
                .invoiceNumber(purchase.getInvoiceNumber())
                .expectedDeliveryDate(purchase.getExpectedDeliveryDate())
                .subtotal(purchase.getSubtotal())
                .igv(purchase.getIgv())
                .total(purchase.getTotal())
                .status(purchase.getStatus())
                .notes(purchase.getNotes())
                .mongoIdSupplier(purchase.getMongoIdSupplier())
                .mongoIdUser(purchase.getMongoIdUser())
                .createdAt(purchase.getCreatedAt())
                .updatedAt(purchase.getUpdatedAt())
                .deletedAt(purchase.getDeletedAt())
                .restoredAt(purchase.getRestoredAt())
                .details(details)
                .build();
    }

    Purchase toEntity(PurchaseDTO dto) {
        return Purchase.builder()
                .invoiceNumber(dto.getInvoiceNumber())
                .purchaseDate(dto.getPurchaseDate() != null ? dto.getPurchaseDate() : LocalDateTime.now())
                .expectedDeliveryDate(dto.getExpectedDeliveryDate())
                .subtotal(dto.getSubtotal())
                .igv(dto.getIgv())
                .total(dto.getTotal())
                .status(dto.getStatus() != null ? dto.getStatus() : AppConstants.STATUS_PENDING)
                .notes(dto.getNotes())
                .mongoIdSupplier(dto.getMongoIdSupplier())
                .mongoIdUser(dto.getMongoIdUser())
                .createdAt(LocalDateTime.now())
                .build();
    }

    PurchaseDetailDTO detailToDTO(PurchaseDetail detail) {
        return PurchaseDetailDTO.builder()
                .idPurchaseDetails(detail.getIdPurchaseDetails())
                .quantity(detail.getQuantity())
                .unitPrice(detail.getUnitPrice())
                .subtotal(detail.getSubtotal())
                .purchasesIdPurchase(detail.getPurchasesIdPurchase())
                .mongoIdProduct(detail.getMongoIdProduct())
                .mongoIdPresentation(detail.getMongoIdPresentation())
                .mongoIdBatch(detail.getMongoIdBatch())
                .build();
    }
}
