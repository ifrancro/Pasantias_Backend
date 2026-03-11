package com.example.herbalife_clubes.dtos.producto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDTO {
    private Integer id;
    private Integer hubId;
    private String hubNombre;
    private Integer clubCreadorId;
    private String clubCreadorNombre;
    private String nombre;
    private String descripcion;
    private String imagenUrl;
    private String ingredientes; // Privado - no se devuelve en endpoints públicos
    private Integer puntosValor;
    private String tipo; // GLOBAL | LOCAL
    private String estadoAprobacion; // APROBADO | PENDIENTE | RECHAZADO
    private Boolean activo;
    /** Disponible en el club (tabla club_productos). Solo se rellena cuando se lista por club para anfitrión/admin. */
    private Boolean disponible;
    private LocalDateTime createdAt;
}

