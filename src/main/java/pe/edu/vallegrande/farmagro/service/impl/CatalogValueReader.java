package pe.edu.vallegrande.farmagro.service.impl;

import lombok.extern.slf4j.Slf4j;
import pe.edu.vallegrande.farmagro.util.AppConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

@Slf4j
final class CatalogValueReader {

    private static final int ISO_DATE_LENGTH = 10;

    private CatalogValueReader() {
    }

    static String entryId(Object catalogEntry) {
        if (!(catalogEntry instanceof Map<?, ?> entryMap)) {
            return null;
        }
        Object entryId = entryMap.get(CatalogFields.ID) != null
                ? entryMap.get(CatalogFields.ID) : entryMap.get(CatalogFields.ALT_ID);
        return entryId == null ? null : entryId.toString();
    }

    static boolean isExpired(Map<String, Object> batch) {
        LocalDate expirationDate = expirationDate(batch);
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    // Seeds and imports store expiry as Date, LocalDate(Time) or ISO text.
    static LocalDate expirationDate(Map<String, Object> batch) {
        return dateValue(batch, CatalogFields.EXPIRATION_DATE);
    }

    static LocalDate manufactureDate(Map<String, Object> batch) {
        return dateValue(batch, CatalogFields.MANUFACTURE_DATE);
    }

    private static LocalDate dateValue(Map<String, Object> batch, String field) {
        Object storedDate = batch.get(field);
        if (storedDate instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (storedDate instanceof LocalDate localDate) {
            return localDate;
        }
        if (storedDate instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        return storedDate instanceof String dateText ? parseIsoDate(dateText) : null;
    }

    static BigDecimal salePrice(Map<String, Object> presentation) {
        Object storedPrice = presentation == null ? null : presentation.get(CatalogFields.SALE_PRICE);
        if (storedPrice instanceof Number number) {
            return toMoney(BigDecimal.valueOf(number.doubleValue()));
        }
        return storedPrice instanceof String priceText ? parsePrice(priceText) : null;
    }

    private static LocalDate parseIsoDate(String dateText) {
        if (dateText.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateText.substring(AppConstants.FIRST_ELEMENT_INDEX, ISO_DATE_LENGTH));
        } catch (DateTimeException | IndexOutOfBoundsException dateParseException) {
            log.warn("Could not parse expiration date '{}': {}", dateText, dateParseException.getMessage());
            return null;
        }
    }

    private static BigDecimal parsePrice(String priceText) {
        if (priceText.isBlank()) {
            return null;
        }
        try {
            return toMoney(new BigDecimal(priceText));
        } catch (NumberFormatException numberFormatException) {
            log.warn("Error parsing price: {}", priceText);
            return null;
        }
    }

    private static BigDecimal toMoney(BigDecimal amount) {
        return amount.setScale(AppConstants.MONETARY_DECIMAL_SCALE, RoundingMode.HALF_UP);
    }
}
