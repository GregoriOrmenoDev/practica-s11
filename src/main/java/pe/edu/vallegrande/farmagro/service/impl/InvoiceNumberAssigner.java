package pe.edu.vallegrande.farmagro.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.vallegrande.farmagro.repository.sql.SaleRepository;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import reactor.core.publisher.Mono;

/**
 * Sale voucher numbers: invoices use the F001 series and receipts B001. When the client sends no
 * number the next correlative of the series is assigned; a number sent by the client must use the
 * series of its voucher type.
 */
@Component
@RequiredArgsConstructor
final class InvoiceNumberAssigner {

    private static final String NUMBER_FORMAT = "%s-%04d";
    private static final String SERIES_SEPARATOR = "-";
    private static final String LIKE_SUFFIX = "-%";

    private final SaleRepository saleRepository;

    String seriesFor(String voucherType) {
        return AppConstants.VOUCHER_INVOICE.equals(voucherType) ? AppConstants.INVOICE_SERIES : AppConstants.RECEIPT_SERIES;
    }

    boolean needsNumber(String invoiceNumber) {
        return invoiceNumber == null || invoiceNumber.isBlank();
    }

    Mono<String> nextNumber(String voucherType) {
        String series = seriesFor(voucherType);
        return saleRepository.findMaxCorrelative(series + LIKE_SUFFIX)
                .defaultIfEmpty(0)
                .map(highest -> NUMBER_FORMAT.formatted(series, highest + 1));
    }

    Mono<Void> validateSeries(String invoiceNumber, String voucherType) {
        String series = seriesFor(voucherType);
        if (invoiceNumber.toUpperCase().startsWith(series + SERIES_SEPARATOR)) {
            return Mono.empty();
        }
        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El comprobante " + invoiceNumber + " debe usar la serie " + series + " ("
                        + (AppConstants.VOUCHER_INVOICE.equals(voucherType) ? "factura" : "boleta") + ")"));
    }
}
