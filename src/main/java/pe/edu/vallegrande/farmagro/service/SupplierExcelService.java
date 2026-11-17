package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.SupplierDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.List;


public interface SupplierExcelService {

    Mono<byte[]> exportToExcel(List<SupplierDTO> suppliers);

    Mono<byte[]> generateTemplate();

    Flux<SupplierDTO> parseExcel(InputStream is);

    Flux<SupplierDTO> parseCsv(InputStream is);
}
