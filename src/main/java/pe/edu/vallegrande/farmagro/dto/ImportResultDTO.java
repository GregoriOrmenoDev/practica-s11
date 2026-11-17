package pe.edu.vallegrande.farmagro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResultDTO {

    
    private int totalRows;

    
    private int importedCount;

    
    private int errorCount;

    
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    
    @Builder.Default
    private List<Object> imported = new ArrayList<>();
}
