package pe.edu.vallegrande.farmagro.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.farmagro.repository.SupplierRepository;
import pe.edu.vallegrande.farmagro.service.SupplierExcelService;
import pe.edu.vallegrande.farmagro.service.SupplierExportService;
import pe.edu.vallegrande.farmagro.service.SupplierPdfService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SupplierExportServiceImpl implements SupplierExportService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    private final SupplierExcelService supplierExcelService;
    private final SupplierPdfService supplierPdfService;

    @Override
    public Mono<byte[]> exportExcel() {
        return supplierRepository.findAll()
                .map(supplierMapper::toDTO)
                .collectList()
                .flatMap(suppliers -> supplierExcelService.exportToExcel(suppliers)
                        .onErrorResume(error -> exportFailure("Excel", error)));
    }

    @Override
    public Mono<byte[]> exportPdf() {
        return supplierRepository.findAll()
                .map(supplierMapper::toDTO)
                .collectList()
                .flatMap(suppliers -> supplierPdfService.exportToPdf(suppliers)
                        .onErrorResume(error -> exportFailure("PDF", error)));
    }

    @Override
    public Mono<byte[]> getExcelTemplate() {
        return supplierExcelService.generateTemplate();
    }

    private Mono<byte[]> exportFailure(String format, Throwable error) {
        return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error al generar el archivo " + format + ": " + error.getMessage(), error));
    }
}
