package pe.edu.vallegrande.farmagro.service;

import reactor.core.publisher.Mono;

public interface ProductExportService {

    Mono<byte[]> exportExcel();

    Mono<byte[]> exportPdf();

    Mono<byte[]> getExcelTemplate();
}
