package com.example.herbalife_clubes.dtos.combo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request para crear o actualizar un combo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComboCreateRequest {
    private String nombre;
    private String descripcion;
    private String imagenUrl;
    /** Si es null, se calcula automáticamente sumando los puntos de los productos. */
    private Integer puntosValor;
    private List<ComboItemRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComboItemRequest {
        private Integer productoId;
        /** Sabor pre-seleccionado (null si el producto no tiene sabores). */
        private Integer saborId;
        private Integer cantidad;
    }
}
