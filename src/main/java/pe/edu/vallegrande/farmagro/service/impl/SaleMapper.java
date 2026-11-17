package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.dto.SaleDTO;
import pe.edu.vallegrande.farmagro.dto.SaleDetailDTO;
import pe.edu.vallegrande.farmagro.model.sql.Sale;
import pe.edu.vallegrande.farmagro.model.sql.SaleDetail;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
final class SaleMapper {

    private static final String DEFAULT_VOUCHER_TYPE = AppConstants.VOUCHER_RECEIPT;

    Sale toEntity(SaleDTO dto) {
        Sale sale = Sale.builder()
                .saleDate(dto.getSaleDate() != null ? dto.getSaleDate() : LocalDateTime.now())
                .status(dto.getStatus() != null ? dto.getStatus() : AppConstants.STATUS_PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        applyFields(sale, dto);
        return sale;
    }

    void applyFields(Sale sale, SaleDTO dto) {
        sale.setInvoiceNumber(dto.getInvoiceNumber());
        sale.setVoucherType(dto.getVoucherType() != null ? dto.getVoucherType() : DEFAULT_VOUCHER_TYPE);
        sale.setPaymentMethod(dto.getPaymentMethod());
        sale.setSubtotal(dto.getSubtotal());
        sale.setDiscount(dto.getDiscount() != null ? dto.getDiscount() : BigDecimal.ZERO);
        sale.setIgv(dto.getIgv() != null ? dto.getIgv() : BigDecimal.ZERO);
        sale.setTotal(dto.getTotal());
        sale.setNotes(dto.getNotes());
        sale.setSalesChannel(dto.getSalesChannel());
        sale.setMongoIdCustomer(dto.getMongoIdCustomer());
        sale.setMongoIdUser(dto.getMongoIdUser());
    }

    void applyUpdateFields(Sale sale, SaleDTO dto) {
        applyFields(sale, dto);
        sale.setUpdatedAt(LocalDateTime.now());
    }

    SaleDetail toDetailEntity(SaleDetailDTO dto, Integer saleId) {
        return SaleDetail.builder()
                .quantity(dto.getQuantity())
                .unitPrice(dto.getUnitPrice())
                .itemDiscount(dto.getItemDiscount() != null ? dto.getItemDiscount() : BigDecimal.ZERO)
                .saleId(saleId)
                .mongoIdProduct(dto.getMongoIdProduct())
                .mongoIdPresentation(dto.getMongoIdPresentation())
                .mongoIdBatch(dto.getMongoIdBatch())
                .build();
    }

    SaleDTO toDTO(Sale sale) {
        return SaleDTO.builder()
                .idSale(sale.getIdSale())
                .saleDate(sale.getSaleDate())
                .invoiceNumber(sale.getInvoiceNumber())
                .voucherType(sale.getVoucherType())
                .paymentMethod(sale.getPaymentMethod())
                .subtotal(sale.getSubtotal())
                .discount(sale.getDiscount())
                .igv(sale.getIgv())
                .total(sale.getTotal())
                .status(sale.getStatus())
                .notes(sale.getNotes())
                .salesChannel(sale.getSalesChannel())
                .mongoIdCustomer(sale.getMongoIdCustomer())
                .mongoIdUser(sale.getMongoIdUser())
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .deletedAt(sale.getDeletedAt())
                .restoredAt(sale.getRestoredAt())
                .build();
    }

    SaleDTO toDTO(Sale sale, List<SaleDetailDTO> details) {
        SaleDTO dto = toDTO(sale);
        dto.setDetails(details);
        return dto;
    }

    SaleDetailDTO toDetailDTO(SaleDetail saleDetail) {
        BigDecimal subtotal = saleDetail.getUnitPrice()
                .multiply(BigDecimal.valueOf(saleDetail.getQuantity()))
                .subtract(saleDetail.getItemDiscount() != null ? saleDetail.getItemDiscount() : BigDecimal.ZERO);

        return SaleDetailDTO.builder()
                .idSaleDetail(saleDetail.getIdSaleDetail())
                .quantity(saleDetail.getQuantity())
                .unitPrice(saleDetail.getUnitPrice())
                .itemDiscount(saleDetail.getItemDiscount())
                .subtotal(subtotal)
                .saleId(saleDetail.getSaleId())
                .mongoIdProduct(saleDetail.getMongoIdProduct())
                .mongoIdPresentation(saleDetail.getMongoIdPresentation())
                .mongoIdBatch(saleDetail.getMongoIdBatch())
                .build();
    }
}
