package pe.edu.vallegrande.farmagro.util;

import java.util.Locale;

public final class TextNormalizer {

    private TextNormalizer() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String trim(String text) {
        return text == null ? null : text.trim();
    }

    public static String trimOrEmpty(String text) {
        return text == null ? AppConstants.EMPTY_TEXT : text.trim();
    }

    public static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    // Stored lower-cased so case-insensitive unique checks stay consistent.
    public static String email(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String code(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    public static String stripExcelDecimal(String numericText) {
        return numericText.trim().replace(AppConstants.EXCEL_DECIMAL_SUFFIX, AppConstants.EMPTY_TEXT);
    }
}
