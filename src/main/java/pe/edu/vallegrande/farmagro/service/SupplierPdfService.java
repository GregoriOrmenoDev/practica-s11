package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.SupplierDTO;
import reactor.core.publisher.Mono;

import java.util.List;


public interface SupplierPdfService {

    Mono<byte[]> exportToPdf(List<SupplierDTO> suppliers);
}
