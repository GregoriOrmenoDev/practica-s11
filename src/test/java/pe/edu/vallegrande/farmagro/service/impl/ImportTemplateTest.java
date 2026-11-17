package pe.edu.vallegrande.farmagro.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.vallegrande.farmagro.model.PersonRole;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportTemplateTest {

    private static final int TEMPLATE_ROWS = 10;

    @Test
    @DisplayName("Supplier template resource parses into complete suppliers")
    void supplierTemplateShouldParse() {
        SupplierExcelServiceImpl service = new SupplierExcelServiceImpl();

        StepVerifier.create(ImportFileSupport.parseCsvResource("templates/supplier-template.csv", service::parseCsv)
                        .collectList())
                .assertNext(suppliers -> {
                    assertEquals(TEMPLATE_ROWS, suppliers.size());
                    assertEquals("Av. Industrial 234, Trujillo", suppliers.getFirst().getAddress());
                    assertEquals(List.of("Herbicidas", "Insecticidas"), suppliers.getFirst().getProductCategories());
                    assertEquals(Boolean.TRUE, suppliers.getFirst().getCertifications().get("iso9001"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Product template resource parses into priced products")
    void productTemplateShouldParse() {
        ProductExcelServiceImpl service = new ProductExcelServiceImpl();

        StepVerifier.create(ImportFileSupport.parseCsvResource("templates/product-template.csv", service::parseCsv)
                        .collectList())
                .assertNext(products -> {
                    assertEquals(TEMPLATE_ROWS, products.size());
                    assertEquals(45.0, products.getFirst().getPrice());
                    assertEquals(List.of("Insecticidas", "Pesticidas"), products.get(1).getCategories());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Person template resource keeps customer data only for customers")
    void personTemplateShouldParse() {
        PersonImportFileParserImpl parser = new PersonImportFileParserImpl();

        StepVerifier.create(ImportFileSupport.parseCsvResource("templates/person-template.csv", parser::parseCsv)
                        .collectList())
                .assertNext(persons -> {
                    assertEquals(TEMPLATE_ROWS, persons.size());
                    assertEquals(PersonRole.VENDOR, persons.get(5).getRole());
                    assertEquals(null, persons.get(5).getCustomer());
                    assertEquals("Jr. Comercio 450, Mala", persons.get(2).getCustomer().getAddress());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Each template generates a non-empty workbook")
    void templatesShouldGenerateWorkbooks() {
        StepVerifier.create(new SupplierExcelServiceImpl().generateTemplate())
                .assertNext(workbook -> assertTrue(workbook.length > 0))
                .verifyComplete();
        StepVerifier.create(new ProductExcelServiceImpl().generateTemplate())
                .assertNext(workbook -> assertTrue(workbook.length > 0))
                .verifyComplete();
        StepVerifier.create(new PersonExcelServiceImpl(new PersonImportFileParserImpl()).generateTemplate())
                .assertNext(workbook -> assertTrue(workbook.length > 0))
                .verifyComplete();
    }

    @Test
    @DisplayName("CSV parser picks the delimiter that yields more header columns")
    void csvParserShouldDetectDelimiter() throws Exception {
        String semicolonCsv = "a;b;c\n\"x, y\";2;3\n";
        String commaCsv = "a,b\n\"x; y\",2\n";

        List<String[]> semicolonRecords = CsvRecordParser.readAll(stream(semicolonCsv));
        List<String[]> commaRecords = CsvRecordParser.readAll(stream(commaCsv));

        assertEquals("x, y", semicolonRecords.get(1)[0]);
        assertEquals(3, semicolonRecords.get(1).length);
        assertEquals("x; y", commaRecords.get(1)[0]);
        assertEquals(2, commaRecords.get(1).length);
    }

    private ByteArrayInputStream stream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
