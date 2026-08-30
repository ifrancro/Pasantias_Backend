package com.example.herbalife_clubes.dtos.combo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComboDTO {
    private Integer id;
    private Integer clubId;
    private String clubNombre;
    private String nombre;
    private String descripcion;
    private String imagenUrl;
    private Integer puntosValor;
    private BigDecimal precio;
    private Boolean activo;
    /** Derivado: activo + precio > 0 + todos los productos pedibles en el club. */
    private Boolean disponible;
    private List<ComboItemDTO> items;
}
