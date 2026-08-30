package com.example.herbalife_clubes.dtos.combo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    /** Precio de venta único del combo (obligatorio en create/update). */
    private BigDecimal precio;
    /**
     * Ignorado en backend: puntosValor se recalcula siempre como suma(producto.puntos * cantidad).
     * Se mantiene el campo por compatibilidad con clientes legacy.
     */
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
