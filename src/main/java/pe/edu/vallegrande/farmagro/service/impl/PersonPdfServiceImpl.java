package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.util.AppConstants;

import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
public class PersonPdfServiceImpl implements pe.edu.vallegrande.farmagro.service.PersonPdfService {

    private static final float[] PERSON_TABLE_COLUMN_WIDTHS = {2.2f, 3.2f, 6.0f, 2.8f, 5.5f, 2.2f, 2.0f};
    private static final String[] PERSON_TABLE_HEADERS = {
            "ID", "Documento", "Nombres / Razón Social", "Teléfono", "Email", "Rol", "Estado"
    };
    private static final MasterCatalogPdfSupport.TableLayout TABLE_LAYOUT =
            new MasterCatalogPdfSupport.TableLayout(PERSON_TABLE_COLUMN_WIDTHS, PERSON_TABLE_HEADERS);

    public Mono<byte[]> exportToPdf(Flux<PersonDTO> persons) {
        return persons.collectList()
                .flatMap(list -> Mono.fromCallable(() -> generatePdf(list)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private byte[] generatePdf(List<PersonDTO> persons) throws Exception {
        MasterCatalogPdfSupport.RowFonts fonts = MasterCatalogPdfSupport.rowFonts();
        return MasterCatalogPdfSupport.generate(
                new MasterCatalogPdfSupport.ReportSpec(
                        "🌱 FARMAGRO S.A.C. - PERSONS MASTER CATALOG",
                        (generatedAt, count) -> "Official report of personnel and clients | Generated on: "
                                + generatedAt + " | Total records: " + count,
                        persons.size(), TABLE_LAYOUT),
                (table, rowIndex) -> addPersonRow(
                        MasterCatalogPdfSupport.catalogRow(table, rowIndex), persons.get(rowIndex), fonts));
    }

    private void addPersonRow(MasterCatalogPdfSupport.CatalogRow row, PersonDTO person,
                              MasterCatalogPdfSupport.RowFonts fonts) {
        String fullName = fullName(person);
        String documentNumber = MasterCatalogPdfSupport.documentLabel(
                person.getDocumentType(), person.getDocumentNumber());
        MasterCatalogPdfSupport.addCell(row, MasterCatalogPdfSupport.valueOrDash(person.getId()),
                fonts.emphasized(), Element.ALIGN_CENTER);
        MasterCatalogPdfSupport.addCell(row, documentNumber, fonts.regular(), Element.ALIGN_CENTER);
        MasterCatalogPdfSupport.addCell(row, fullName, fonts.regular(), Element.ALIGN_LEFT);
        MasterCatalogPdfSupport.addCell(row, MasterCatalogPdfSupport.valueOrDash(person.getPhone()),
                fonts.regular(), Element.ALIGN_CENTER);
        MasterCatalogPdfSupport.addCell(row, MasterCatalogPdfSupport.valueOrDash(person.getEmail()),
                fonts.regular(), Element.ALIGN_LEFT);
        MasterCatalogPdfSupport.addCell(row,
                person.getRole() != null ? person.getRole().toStorageValue() : AppConstants.MISSING_VALUE_PLACEHOLDER,
                fonts.emphasized(), Element.ALIGN_CENTER);
        MasterCatalogPdfSupport.addCell(row, MasterCatalogPdfSupport.statusLabel(person.getStatus()),
                fonts.regular(), Element.ALIGN_CENTER);
    }

    // The RUC placeholder last name is left out of the printed name.
    private String fullName(PersonDTO person) {
        String lastNames = person.getLastNames();
        boolean hasLastNames = lastNames != null && !AppConstants.MISSING_VALUE_PLACEHOLDER.equals(lastNames);
        return hasLastNames ? person.getFirstNames() + " " + lastNames : person.getFirstNames();
    }
}
