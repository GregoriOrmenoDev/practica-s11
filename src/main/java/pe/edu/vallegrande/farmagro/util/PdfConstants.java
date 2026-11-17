package pe.edu.vallegrande.farmagro.util;

public final class PdfConstants {

    private PdfConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static final float TABLE_WIDTH_PERCENTAGE = 100f;

    public static final String PAGE_SIZE = "A4";

    public static final float MARGIN_LEFT = 36f;

    public static final float MARGIN_RIGHT = 36f;

    public static final float MARGIN_TOP = 36f;

    public static final float MARGIN_BOTTOM = 36f;

    public static final float MASTER_CATALOG_MARGIN_LEFT = 24f;
    public static final float MASTER_CATALOG_MARGIN_RIGHT = 24f;
    public static final float MASTER_CATALOG_MARGIN_TOP = 30f;
    public static final float MASTER_CATALOG_MARGIN_BOTTOM = 30f;

    public static final float PRODUCT_REPORT_MARGIN_LEFT = 20f;
    public static final float PRODUCT_REPORT_MARGIN_RIGHT = 20f;
    public static final float PRODUCT_REPORT_MARGIN_TOP = 28f;
    public static final float PRODUCT_REPORT_MARGIN_BOTTOM = 28f;

    public static final int REPORT_TITLE_FONT_SIZE = 18;
    public static final int PRODUCT_REPORT_TITLE_FONT_SIZE = 17;
    public static final int REPORT_SUBTITLE_FONT_SIZE = 10;
    public static final int PRODUCT_REPORT_SUBTITLE_FONT_SIZE = 9;
    public static final int REPORT_TABLE_HEADER_FONT_SIZE = 10;
    public static final int PRODUCT_REPORT_TABLE_HEADER_FONT_SIZE = 9;
    public static final int REPORT_ROW_FONT_SIZE = 9;
    public static final int PRODUCT_REPORT_ROW_FONT_SIZE = 8;
    public static final float PRODUCT_REPORT_SMALL_FONT_SIZE = 7.5f;
    public static final int REPORT_FOOTER_FONT_SIZE = 8;
    public static final float PRODUCT_REPORT_FOOTER_FONT_SIZE = 7.5f;
    public static final int PRODUCT_LEGEND_FONT_SIZE = 8;

    public static final int REPORT_TITLE_SPACING = 4;
    public static final int REPORT_SUBTITLE_SPACING = 16;
    public static final int PRODUCT_SUBTITLE_SPACING = 6;
    public static final int PRODUCT_LEGEND_SPACING = 10;
    public static final int REPORT_HEADER_CELL_PADDING = 8;
    public static final int PRODUCT_HEADER_CELL_PADDING = 7;
    public static final int REPORT_BODY_CELL_PADDING = 6;
    public static final int ALTERNATING_ROW_INTERVAL = 2;
    public static final int FIRST_ROW_INDEX = 0;

    public static final int MASTER_CATALOG_TABLE_COLUMNS = 7;
    public static final int PRODUCT_TABLE_COLUMNS = 9;
    public static final String REPORT_TIMESTAMP_PATTERN = "dd/MM/yyyy HH:mm:ss";
    public static final String PRODUCT_PRICE_FORMAT = "%.2f";
}
