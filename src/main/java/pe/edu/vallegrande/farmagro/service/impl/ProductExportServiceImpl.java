package pe.edu.vallegrande.farmagro.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.farmagro.dto.ProductDTO;
import pe.edu.vallegrande.farmagro.repository.ProductRepository;
import pe.edu.vallegrande.farmagro.service.ProductExcelService;
import pe.edu.vallegrande.farmagro.service.ProductExportService;
import pe.edu.vallegrande.farmagro.service.ProductPdfService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductExportServiceImpl implements ProductExportService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductExcelService productExcelService;
    private final ProductPdfService productPdfService;

    @Override
    public Mono<byte[]> exportExcel() {
        return loadProducts()
                .flatMap(products -> productExcelService.exportToExcel(products)
                        .onErrorMap(error -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Error al generar el Excel de productos: " + error.getMessage())));
    }

    @Override
    public Mono<byte[]> exportPdf() {
        return loadProducts()
                .flatMap(products -> productPdfService.exportToPdf(products)
                        .onErrorMap(error -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Error al generar el PDF de productos: " + error.getMessage())));
    }

    @Override
    public Mono<byte[]> getExcelTemplate() {
        return productExcelService.generateTemplate();
    }

    private Mono<List<ProductDTO>> loadProducts() {
        return productRepository.findAll()
                .map(productMapper::toDTO)
                .collectList();
    }
}
