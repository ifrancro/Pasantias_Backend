package com.example.herbalife_clubes.dtos.sabor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que combina la info del sabor con su disponibilidad en un club/producto específico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaborDisponibilidadDTO {
    private Integer id;
    private String nombre;
    /** true = el anfitrión tiene este sabor activo para este producto en su club. */
    private Boolean disponible;
}
