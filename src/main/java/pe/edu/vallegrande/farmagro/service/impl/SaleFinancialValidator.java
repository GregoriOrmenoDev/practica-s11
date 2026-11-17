package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.dto.SaleDTO;
import pe.edu.vallegrande.farmagro.dto.SaleDetailDTO;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
final class SaleFinancialValidator {


    Mono<Void> validate(SaleDTO dto) {
        return calculateAndValidateSubtotal(dto)
                .then(validateDiscount(dto, dto.getSubtotal()))
                .then(validateTaxes(dto, dto.getSubtotal()))
                .flatMap(taxableBase -> validateTotal(dto, taxableBase));
    }

    private Mono<BigDecimal> calculateAndValidateSubtotal(SaleDTO dto) {
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        BigDecimal itemDiscountTotal = BigDecimal.ZERO;
        for (SaleDetailDTO detail : dto.getDetails()) {
            BigDecimal lineAmount = detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
            BigDecimal itemDiscount = detail.getItemDiscount() != null ? detail.getItemDiscount() : BigDecimal.ZERO;
            if (itemDiscount.compareTo(lineAmount) > 0) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El descuento del ítem (" + itemDiscount + ") no puede superar el monto de la línea (" + lineAmount + ")"));
            }
            calculatedSubtotal = calculatedSubtotal.add(lineAmount);
            itemDiscountTotal = itemDiscountTotal.add(itemDiscount);
        }
        if (dto.getSubtotal().subtract(calculatedSubtotal).abs().compareTo(AppConstants.PRICE_TOLERANCE) > 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El subtotal de la venta (" + dto.getSubtotal() + ") no coincide con la suma de los details (" + calculatedSubtotal + ")"));
        }
        BigDecimal headerDiscount = dto.getDiscount() != null ? dto.getDiscount() : BigDecimal.ZERO;
        if (headerDiscount.subtract(itemDiscountTotal).abs().compareTo(AppConstants.PRICE_TOLERANCE) > 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El descuento general debe coincidir con la suma de descuentos por ítem"));
        }
        return Mono.just(calculatedSubtotal);
    }

    private Mono<Void> validateDiscount(SaleDTO dto, BigDecimal subtotal) {
        BigDecimal discount = dto.getDiscount() != null ? dto.getDiscount() : BigDecimal.ZERO;
        if (discount.compareTo(subtotal) > 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El descuento general (" + discount + ") no puede ser mayor al subtotal de la venta (" + subtotal + ")"));
        }
        return Mono.empty();
    }

    private Mono<BigDecimal> validateTaxes(SaleDTO dto, BigDecimal subtotal) {
        BigDecimal discount = dto.getDiscount() != null ? dto.getDiscount() : BigDecimal.ZERO;
        BigDecimal taxableBase = subtotal.subtract(discount);
        BigDecimal expectedIgv = taxableBase.multiply(AppConstants.IGV_RATE)
                .setScale(AppConstants.MONETARY_DECIMAL_SCALE, RoundingMode.HALF_UP);
        if (dto.getIgv() == null) {
            dto.setIgv(expectedIgv);
        } else if (dto.getIgv().subtract(expectedIgv).abs().compareTo(AppConstants.PRICE_TOLERANCE) > 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El monto de IGV (S/ " + dto.getIgv() + ") no coincide con la tasa oficial del "
                            + AppConstants.IGV_PERCENT_LABEL + " (S/ " + expectedIgv + ")"));
        }
        return Mono.just(taxableBase);
    }

    private Mono<Void> validateTotal(SaleDTO dto, BigDecimal taxableBase) {
        BigDecimal calculatedTotal = taxableBase.add(dto.getIgv());
        if (dto.getTotal().subtract(calculatedTotal).abs().compareTo(AppConstants.PRICE_TOLERANCE) > 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El total de la venta (" + dto.getTotal() + ") no coincide con subtotal - descuento + IGV (" + calculatedTotal + ")"));
        }
        return Mono.empty();
    }
}
