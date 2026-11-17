package pe.edu.vallegrande.farmagro.model.sql;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("UBIGEO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeographicLocation {

    @Id
    @Column("id_ubigeo")
    private Integer geographicLocationId;

    @Column("departament")
    private String department;

    @Column("province")
    private String province;

    @Column("district")
    private String district;
}
