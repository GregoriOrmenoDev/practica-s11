package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.dto.SupplierDTO;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
public class SupplierPdfServiceImpl implements pe.edu.vallegrande.farmagro.service.SupplierPdfService {

    private static final float[] SUPPLIER_TABLE_COLUMN_WIDTHS = {2.0f, 2.5f, 5.5f, 3.0f, 5.0f, 3.5f, 2.0f};
    private static final String[] SUPPLIER_TABLE_HEADERS = {
            "ID", "Documento", "Razón Social", "Teléfono", "Email", "Dirección", "Estado"
    };
    private static final MasterCatalogPdfSupport.TableLayout TABLE_LAYOUT =
            new MasterCatalogPdfSupport.TableLayout(SUPPLIER_TABLE_COLUMN_WIDTHS, SUPPLIER_TABLE_HEADERS);

    @Override
    public Mono<byte[]> exportToPdf(List<SupplierDTO> suppliers) {
        return Mono.fromCallable(() -> generatePdf(suppliers))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private byte[] generatePdf(List<SupplierDTO> suppliers) throws Exception {
        MasterCatalogPdfSupport.RowFonts fonts = MasterCatalogPdfSupport.rowFonts();
        return MasterCatalogPdfSupport.generate(
                new MasterCatalogPdfSupport.ReportSpec(
                        "🌱 FARMAGRO S.A.C. - SUPPLIERS MASTER CATALOG",
                        (generatedAt, count) -> "Reporte oficial de proveedores | Fecha de emisión: " + generatedAt
                                + " | Total registros: " + count,
                        suppliers.size(), TABLE_LAYOUT),
                (table, rowIndex) -> addSupplierRow(
                        MasterCatalogPdfSupport.catalogRow(table, rowIndex), suppliers.get(rowIndex), fonts));
    }

    private void addSupplierRow(MasterCatalogPdfSupport.CatalogRow row, SupplierDTO supplier,
                                MasterCatalogPdfSupport.RowFonts fonts) {
        String documentNumber = MasterCatalogPdfSupport.documentLabel(
                supplier.getDocumentType(), supplier.getDocumentNumber());
        MasterCatalogPdfSupport.addCell(row, MasterCatalogPdfSupport.valueOrDash(supplier.getId()),
                fonts.emphasized(), Element.ALIGN_CENTER);
        MasterCatalogPdfSupport.addCell(row, documentNumber, fonts.regular(), Element.ALIGN_CENTER);
        MasterCatalogPdfSupport.addCell(row, MasterCatalogPdfSupport.valueOrDash(supplier.getBusinessName()),
                fonts.regular(), Element.ALIGN_LEFT);
        MasterCatalogPdfSupport.addCell(row, MasterCatalogPdfSupport.valueOrDash(supplier.getPhone()),
                fonts.regular(), Element.ALIGN_CENTER);
        MasterCatalogPdfSupport.addCell(row, MasterCatalogPdfSupport.valueOrDash(supplier.getEmail()),
                fonts.regular(), Element.ALIGN_LEFT);
        MasterCatalogPdfSupport.addCell(row, MasterCatalogPdfSupport.valueOrDash(supplier.getAddress()),
                fonts.regular(), Element.ALIGN_LEFT);
        MasterCatalogPdfSupport.addCell(row, MasterCatalogPdfSupport.statusLabel(supplier.getStatus()),
                fonts.regular(), Element.ALIGN_CENTER);
    }
}
