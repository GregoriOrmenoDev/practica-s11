package pe.edu.vallegrande.farmagro.util;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.math.BigInteger;

public final class AppConstants {

    private AppConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // Bound default list endpoints to avoid unbounded collection reads.
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final String DEFAULT_PAGE_VALUE = "0";
    public static final String DEFAULT_PAGE_SIZE_VALUE = "20";
    public static final int DEFAULT_QUERY_LIMIT = 100;
    // Catalog and purchase lists default to the old 100-row cap so clients that do not page see the same data.
    public static final String DEFAULT_LIST_PAGE_SIZE_VALUE = "100";

    // IGV: Peru's sales tax, added on top of the taxable subtotal.
    public static final BigDecimal IGV_RATE = new BigDecimal("0.18");
    private static final BigDecimal PERCENT_FACTOR = BigDecimal.valueOf(100);
    public static final String IGV_PERCENT_LABEL =
            IGV_RATE.multiply(PERCENT_FACTOR).stripTrailingZeros().toPlainString() + "%";

    // Absolute tolerance of S/ 0.05 (not a percentage) when comparing prices and totals with the expected values.
    public static final BigDecimal PRICE_TOLERANCE = new BigDecimal("0.05");
    public static final int MONETARY_DECIMAL_SCALE = 2;

    public static final int FIRST_ELEMENT_INDEX = 0;

    // New products start without stock: units only enter through received purchases.
    public static final int DEFAULT_STOCK = 0;
    public static final String DEFAULT_TOXICITY_LEVEL = "IV";
    public static final String DEFAULT_SUPPLIER_ID = "SUP001";
    public static final String INVENTORY_KEY_SEPARATOR = "#";

    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String DELIVERY_TYPE_DELIVERY = "Delivery";
    // Business calendar (Lima). Containers run in UTC, so business dates are computed in this zone explicitly.
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");
    public static final String STATUS_ON_THE_WAY = "ON_THE_WAY";

    public static final int MINIMUM_VALUE = 0;
    public static final int MINIMUM_ID = 1;
    public static final int MINIMUM_QUANTITY = 1;
    public static final int MINIMUM_ITEMS = 1;
    public static final int MINIMUM_DESCRIPTION_LENGTH = 10;
    public static final int MAXIMUM_DESCRIPTION_LENGTH = 300;
    public static final String MINIMUM_POSITIVE_AMOUNT = "0.01";
    public static final String MINIMUM_NON_NEGATIVE_AMOUNT = "0.0";

    public static final String DOCUMENT_TYPE_DNI = "DNI";
    public static final String DOCUMENT_TYPE_RUC = "RUC";
    public static final String DOCUMENT_TYPE_CE = "CE";
    public static final String DOCUMENT_TYPE_PASSPORT = "PAS";

    public static final String VOUCHER_INVOICE = "INVOICE";
    public static final String VOUCHER_RECEIPT = "RECEIPT";
    // Voucher series: invoices (factura) use F001 and receipts (boleta) B001.
    public static final String INVOICE_SERIES = "F001";
    public static final String RECEIPT_SERIES = "B001";

    public static final String EMPTY_TEXT = "";
    // RUC last names and blank export cells use this placeholder.
    public static final String MISSING_VALUE_PLACEHOLDER = "-";
    // POI reads numeric cells as doubles, e.g. "12345678.0".
    public static final String EXCEL_DECIMAL_SUFFIX = ".0";

    // 3-letter prefix plus digits, stored in SQL Server VARCHAR(10) columns.
    public static final int MIN_ID_DIGITS = 3;
    public static final int MAX_ID_DIGITS = 7;
    public static final int ID_SEQUENCE_STEP = 1;
    public static final int MAX_ID_SEQUENCE = BigInteger.TEN.pow(MAX_ID_DIGITS).intValueExact() - ID_SEQUENCE_STEP;
    public static final String PERSON_ID_PREFIX = "PER";
    public static final String PRODUCT_ID_PREFIX = "PRD";
    public static final String PRESENTATION_ID_PREFIX = "PST";
    public static final String BATCH_ID_PREFIX = "LOT";
    public static final String SUPPLIER_ID_PREFIX = "SUP";
    private static final String ID_SEQUENCE_PATTERN = "[0-9]{" + MIN_ID_DIGITS + "," + MAX_ID_DIGITS + "}$";
    public static final String PERSON_ID_PATTERN = "^" + PERSON_ID_PREFIX + ID_SEQUENCE_PATTERN;
    public static final String PRODUCT_ID_PATTERN = "^" + PRODUCT_ID_PREFIX + ID_SEQUENCE_PATTERN;
    public static final String PRESENTATION_ID_PATTERN = "^" + PRESENTATION_ID_PREFIX + ID_SEQUENCE_PATTERN;
    public static final String BATCH_ID_PATTERN = "^" + BATCH_ID_PREFIX + ID_SEQUENCE_PATTERN;
    public static final String SUPPLIER_ID_PATTERN = "^" + SUPPLIER_ID_PREFIX + ID_SEQUENCE_PATTERN;
}
