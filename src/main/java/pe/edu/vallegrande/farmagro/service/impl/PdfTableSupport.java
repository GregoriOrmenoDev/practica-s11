package pe.edu.vallegrande.farmagro.service.impl;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.awt.Color;

final class PdfTableSupport {

    record CellBorder(int padding, Color color) {
    }

    record CellAppearance(Font font, Color background, int alignment, CellBorder border) {
    }

    private PdfTableSupport() {
    }

    static void addCell(PdfPTable table, String text, CellAppearance appearance) {
        PdfPCell cell = new PdfPCell(new Phrase(text, appearance.font()));
        cell.setBackgroundColor(appearance.background());
        cell.setHorizontalAlignment(appearance.alignment());
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(appearance.border().padding());
        cell.setBorderColor(appearance.border().color());
        table.addCell(cell);
    }
}
