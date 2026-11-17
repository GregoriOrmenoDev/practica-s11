package pe.edu.vallegrande.farmagro.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;

final class PdfDocumentSupport {

    @FunctionalInterface
    interface ReportWriter {
        void write(Document document) throws Exception;
    }

    record PageMargins(float left, float right, float top, float bottom) {
    }

    private PdfDocumentSupport() {
    }

    static byte[] generate(PageMargins margins, ReportWriter reportWriter) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(),
                    margins.left(), margins.right(), margins.top(), margins.bottom());
            try {
                PdfWriter.getInstance(document, output);
                document.open();
                reportWriter.write(document);
            } finally {
                if (document.isOpen()) document.close();
            }
            return output.toByteArray();
        }
    }
}
