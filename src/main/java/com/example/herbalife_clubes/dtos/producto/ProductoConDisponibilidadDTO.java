package com.example.herbalife_clubes.dtos.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para productos que incluye información de disponibilidad en un club específico.
 * Usado para el catálogo del Hub donde el anfitrión puede ver todos los productos
 * y su estado de disponibilidad en su club.
 * Incluye tipo (GLOBAL/LOCAL) y estadoAprobacion para filtrado y agrupación en front.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoConDisponibilidadDTO {
    private Integer id;
    private Integer hubId;
    private String hubNombre;
    private String nombre;
    private String descripcion;
    private String tipo;           // GLOBAL | LOCAL
    private Boolean esCombo;       // true si el producto es un Combo
    private String estadoAprobacion; // APROBADO | PENDIENTE | RECHAZADO
    private Boolean activo;        // Estado global del producto
    private Boolean disponible;    // Estado local en el club (null si no existe relación)
    private LocalDateTime createdAt;
}

