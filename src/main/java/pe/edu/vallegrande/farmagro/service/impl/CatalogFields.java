package pe.edu.vallegrande.farmagro.service.impl;

final class CatalogFields {

    static final String ID = "_id";
    static final String ALT_ID = "id";
    static final String BATCHES = "batches";
    static final String CURRENT_STOCK = "current_stock";
    static final String BATCH_STOCK = "batch_stock";
    static final String SALE_PRICE = "sale_price";
    static final String SUPPLIER_ID = "mongo_id_supplier";
    static final String STATUS = "status";
    static final String LABEL = "label";
    static final String CONTENT = "content";
    static final String UNIT_OF_MEASURE = "unit_of_measure";
    static final String BARCODE = "barcode";
    static final String CREATED_AT = "created_at";
    static final String NAME = "name";
    static final String ABBREVIATION = "abbreviation";
    static final String BATCH_NUMBER = "batch_number";
    static final String MANUFACTURE_DATE = "manufacture_date";
    static final String EXPIRATION_DATE = "expiration_date";

    private CatalogFields() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
