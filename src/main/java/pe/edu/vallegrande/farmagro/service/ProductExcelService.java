package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.ProductDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.List;


public interface ProductExcelService {

    Mono<byte[]> exportToExcel(List<ProductDTO> products);

    Mono<byte[]> generateTemplate();

    Flux<ProductDTO> parseExcel(InputStream is);

    Flux<ProductDTO> parseCsv(InputStream is);
}
