package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.dto.PurchaseDTO;
import pe.edu.vallegrande.farmagro.dto.PurchaseDetailDTO;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
final class PurchaseFinancialValidator {

    Mono<Void> validate(PurchaseDTO dto) {
        BigDecimal calculatedSubtotal = dto.getDetails().stream()
                .map(PurchaseDetailDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return validateSubtotal(dto, calculatedSubtotal)
                .then(validateIgv(dto, calculatedSubtotal))
                .then(validateTotal(dto, calculatedSubtotal));
    }

    private Mono<Void> validateSubtotal(PurchaseDTO dto, BigDecimal calculatedSubtotal) {
        if (dto.getSubtotal().subtract(calculatedSubtotal).abs().compareTo(AppConstants.PRICE_TOLERANCE) > 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El subtotal de compra no coincide con la suma de los detalles"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateIgv(PurchaseDTO dto, BigDecimal calculatedSubtotal) {
        BigDecimal calculatedIgv = calculatedSubtotal.multiply(AppConstants.IGV_RATE)
                .setScale(AppConstants.MONETARY_DECIMAL_SCALE, RoundingMode.HALF_UP);
        if (dto.getIgv().subtract(calculatedIgv).abs().compareTo(AppConstants.PRICE_TOLERANCE) > 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El IGV de compra no coincide con el " + AppConstants.IGV_PERCENT_LABEL + " del subtotal"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateTotal(PurchaseDTO dto, BigDecimal calculatedSubtotal) {
        BigDecimal calculatedIgv = calculatedSubtotal.multiply(AppConstants.IGV_RATE)
                .setScale(AppConstants.MONETARY_DECIMAL_SCALE, RoundingMode.HALF_UP);
        BigDecimal calculatedTotal = calculatedSubtotal.add(calculatedIgv);
        if (dto.getTotal().subtract(calculatedTotal).abs().compareTo(AppConstants.PRICE_TOLERANCE) > 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El total de compra no coincide con subtotal más IGV"));
        }
        return Mono.empty();
    }
}
