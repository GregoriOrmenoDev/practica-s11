package pe.edu.vallegrande.farmagro.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImportDefaultsTest {

    @Test
    @DisplayName("An unknown toxicity is kept for the validation to reject, never turned into category IV")
    void productImportShouldNotDowngradeToxicity() {
        String csv = "id;name;brand;sanitaryRegistration;description;activeIngredient;toxicityLevel;requiresLicense;categories;price;imageUrl\n"
                + ";Producto A;Marca;;Descripcion valida del producto A;Ingrediente;Ia;false;Herbicidas;10.00;\n"
                + ";Producto B;Marca;;Descripcion valida del producto B;Ingrediente;;false;Herbicidas;10.00;\n";

        StepVerifier.create(new ProductExcelServiceImpl().parseCsv(stream(csv)).collectList())
                .assertNext(products -> {
                    assertEquals("IA", products.get(0).getToxicityLevel());
                    assertEquals("IV", products.get(1).getToxicityLevel());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("A supplier row without UBIGEO stays empty so the import reports it")
    void supplierImportShouldNotInventLocation() {
        String csv = "id;documentType;documentNumber;businessName;contactName;phone;email;address;geographicLocationId;productCategories;iso9001;senasaReg\n"
                + ";RUC;20501234567;Agro S.A.C.;Pedro;987654321;ventas@agro.pe;Av. Uno 1;;Herbicidas;true;RS-001\n";

        StepVerifier.create(new SupplierExcelServiceImpl().parseCsv(stream(csv)).collectList())
                .assertNext(suppliers -> assertNull(suppliers.get(0).getGeographicLocationId()))
                .verifyComplete();
    }

    private InputStream stream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
