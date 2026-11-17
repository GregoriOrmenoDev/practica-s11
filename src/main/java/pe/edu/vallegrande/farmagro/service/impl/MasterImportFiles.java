package pe.edu.vallegrande.farmagro.service.impl;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Locale;

final class MasterImportFiles {

    private MasterImportFiles() {
    }

    static Mono<DataBuffer> joinContent(FilePart file) {
        String filename = file.filename();
        if (filename == null || !isSupported(filename)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Formato no compatible; use un archivo .xlsx o .csv"));
        }
        return DataBufferUtils.join(file.content())
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El archivo está vacío")));
    }

    static byte[] readBytesAndRelease(DataBuffer buffer) {
        byte[] fileBytes = new byte[buffer.readableByteCount()];
        try {
            buffer.read(fileBytes);
            return fileBytes;
        } finally {
            DataBufferUtils.release(buffer);
        }
    }

    static boolean isCsv(FilePart file) {
        return file.filename().toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    private static boolean isSupported(String filename) {
        String normalizedFilename = filename.toLowerCase(Locale.ROOT);
        return normalizedFilename.endsWith(".xlsx") || normalizedFilename.endsWith(".csv");
    }
}
