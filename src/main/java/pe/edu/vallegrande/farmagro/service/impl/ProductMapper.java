package pe.edu.vallegrande.farmagro.service.impl;

import lombok.extern.slf4j.Slf4j;
import pe.edu.vallegrande.farmagro.dto.ProductDTO;
import pe.edu.vallegrande.farmagro.model.Product;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
final class ProductMapper {

    ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .sanitaryRegistration(product.getSanitaryRegistration())
                .description(product.getDescription())
                .activeIngredient(product.getActiveIngredient())
                .toxicityLevel(product.getToxicityLevel())
                .requiresLicense(product.getRequiresLicense())
                .categories(product.getCategories())
                .imageUrl(product.getImageUrl())
                .technicalSpecifications(product.getTechnicalSpecifications())
                .presentations(product.getPresentations())
                .price(extractPresentationPrice(product))
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .deletedAt(product.getDeletedAt())
                .restoredAt(product.getRestoredAt())
                .build();
    }

    private Double extractPresentationPrice(Product product) {
        Double price = null;
        if (product.getPresentations() != null && !product.getPresentations().isEmpty()) {
            Object first = product.getPresentations().get(AppConstants.FIRST_ELEMENT_INDEX);
            if (first instanceof Map<?, ?> presentationMap) {
                Object salePrice = presentationMap.get(CatalogFields.SALE_PRICE);
                if (salePrice instanceof Number numericPrice) {
                    price = numericPrice.doubleValue();
                } else if (salePrice instanceof String priceText) {
                    try {
                        price = Double.parseDouble(priceText);
                    } catch (NumberFormatException exception) {
                        log.warn("Error parsing presentation price: {}", priceText);
                    }
                }
            }
        }
        return price;
    }
}
