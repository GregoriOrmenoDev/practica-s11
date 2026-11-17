package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.ProductDTO;
import reactor.core.publisher.Mono;

import java.util.List;


public interface ProductPdfService {

    Mono<byte[]> exportToPdf(List<ProductDTO> products);
}
