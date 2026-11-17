package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.dto.ProductDTO;
import pe.edu.vallegrande.farmagro.model.Product;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Component
final class ProductEntityFactory {

    private static final int RANDOM_ID_BOUND = AppConstants.MAX_ID_SEQUENCE + AppConstants.ID_SEQUENCE_STEP;
    private static final String FULL_WIDTH_SEQUENCE_FORMAT = "%0" + AppConstants.MAX_ID_DIGITS + "d";
    private static final String PRESENTATION_ID_FORMAT = AppConstants.PRESENTATION_ID_PREFIX + FULL_WIDTH_SEQUENCE_FORMAT;
    private static final String BATCH_ID_FORMAT = AppConstants.BATCH_ID_PREFIX + FULL_WIDTH_SEQUENCE_FORMAT;
    // 775 is the GS1 country prefix assigned to Peru for EAN barcodes.
    private static final String PERU_GS1_PREFIX = "775";
    private static final String BARCODE_FORMAT = PERU_GS1_PREFIX + FULL_WIDTH_SEQUENCE_FORMAT;
    private static final String DEFAULT_PRESENTATION_LABEL = "Standard Presentation 1L";
    private static final double DEFAULT_PRESENTATION_CONTENT = 1.0;
    private static final String DEFAULT_UNIT_NAME = "Litros";
    private static final String DEFAULT_UNIT_ABBREVIATION = "L";
    private static final String DEFAULT_BATCH_NUMBER_PREFIX = "BATCH-INIT-";
    private static final int DEFAULT_EXPIRATION_YEARS = 3;

    Product toEntity(ProductDTO dto, String id) {
        List<Object> presentations = dto.getPresentations();
        if ((presentations == null || presentations.isEmpty()) && dto.getPrice() != null
                && dto.getPrice() > AppConstants.MINIMUM_VALUE) {
            presentations = List.of(createDefaultPresentation(dto.getPrice()));
        }

        return Product.builder()
                .id(id)
                .name(dto.getName())
                .brand(dto.getBrand())
                .sanitaryRegistration(blankToNull(dto.getSanitaryRegistration()))
                .description(dto.getDescription())
                .activeIngredient(dto.getActiveIngredient())
                .toxicityLevel(dto.getToxicityLevel() != null
                        ? dto.getToxicityLevel() : AppConstants.DEFAULT_TOXICITY_LEVEL)
                .requiresLicense(Boolean.TRUE.equals(dto.getRequiresLicense()))
                .categories(dto.getCategories())
                .imageUrl(dto.getImageUrl())
                .technicalSpecifications(dto.getTechnicalSpecifications())
                .presentations(presentations)
                .status(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Map<String, Object> createDefaultPresentation(Double price) {
        Map<String, Object> defaultPres = new HashMap<>();
        defaultPres.put(CatalogFields.ID, String.format(PRESENTATION_ID_FORMAT, randomId()));
        defaultPres.put(CatalogFields.LABEL, DEFAULT_PRESENTATION_LABEL);
        defaultPres.put(CatalogFields.CONTENT, DEFAULT_PRESENTATION_CONTENT);
        defaultPres.put(CatalogFields.UNIT_OF_MEASURE, createDefaultUnit());
        defaultPres.put(CatalogFields.SALE_PRICE, price);
        defaultPres.put(CatalogFields.CURRENT_STOCK, AppConstants.DEFAULT_STOCK);
        defaultPres.put(CatalogFields.BARCODE, String.format(BARCODE_FORMAT, randomId()));
        defaultPres.put(CatalogFields.SUPPLIER_ID, AppConstants.DEFAULT_SUPPLIER_ID);
        defaultPres.put(CatalogFields.STATUS, true);
        defaultPres.put(CatalogFields.CREATED_AT, new Date());
        defaultPres.put(CatalogFields.BATCHES, List.of(createDefaultBatch()));
        return defaultPres;
    }

    private Map<String, String> createDefaultUnit() {
        Map<String, String> unit = new HashMap<>();
        unit.put(CatalogFields.NAME, DEFAULT_UNIT_NAME);
        unit.put(CatalogFields.ABBREVIATION, DEFAULT_UNIT_ABBREVIATION);
        return unit;
    }

    private Map<String, Object> createDefaultBatch() {
        Map<String, Object> defaultBatch = new HashMap<>();
        defaultBatch.put(CatalogFields.ID, String.format(BATCH_ID_FORMAT, randomId()));
        defaultBatch.put(CatalogFields.BATCH_NUMBER, DEFAULT_BATCH_NUMBER_PREFIX + LocalDate.now().getYear());
        defaultBatch.put(CatalogFields.MANUFACTURE_DATE, new Date());

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, DEFAULT_EXPIRATION_YEARS);
        defaultBatch.put(CatalogFields.EXPIRATION_DATE, calendar.getTime());
        defaultBatch.put(CatalogFields.BATCH_STOCK, AppConstants.DEFAULT_STOCK);
        defaultBatch.put(CatalogFields.STATUS, true);
        return defaultBatch;
    }

    // Blank registrations are stored as null so the partial unique index ignores them.
    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    /**
     * Every stored presentation carries a barcode: the unique index on presentations.barcode is
     * multikey, so a presentation without one would index null and collide with any other product
     * that also has one presentation without barcode. Missing or blank barcodes get a generated one.
     */
    List<Object> withBarcodes(List<Object> presentations) {
        if (presentations == null) return null;
        List<Object> completed = new ArrayList<>(presentations.size());
        for (Object presentationValue : presentations) {
            if (presentationValue instanceof Map<?, ?> presentation && isBlank(presentation.get(CatalogFields.BARCODE))) {
                Map<String, Object> withBarcode = new HashMap<>(CatalogInventorySupport.asStringMap(presentation));
                withBarcode.put(CatalogFields.BARCODE, String.format(BARCODE_FORMAT, randomId()));
                completed.add(withBarcode);
            } else {
                completed.add(presentationValue);
            }
        }
        return completed;
    }

    /** A LOT id not used by any batch of the product (batch ids are looked up inside their product). */
    String newBatchId(Product product) {
        Set<String> used = new HashSet<>();
        if (product.getPresentations() != null) {
            for (Object presentationValue : product.getPresentations()) {
                if (presentationValue instanceof Map<?, ?> presentation && presentation.get(CatalogFields.BATCHES) instanceof List<?> batches) {
                    batches.stream().filter(Map.class::isInstance)
                            .map(batch -> CatalogValueReader.entryId((Map<?, ?>) batch)).forEach(used::add);
                }
            }
        }
        String candidate;
        do {
            candidate = String.format(BATCH_ID_FORMAT, randomId());
        } while (used.contains(candidate));
        return candidate;
    }

    private static boolean isBlank(Object value) {
        return value == null || value.toString().isBlank();
    }

    private int randomId() {
        return ThreadLocalRandom.current().nextInt(RANDOM_ID_BOUND);
    }
}
