package com.example.herbalife_clubes.dtos.producto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    /** Precio BASE / sugerido del producto ({@code productos.precio}). No es el override del club. */
    private BigDecimal precio;
    /**
     * Precio de venta en el club: override si existe, si no el precio base.
     * Solo se rellena en vistas con contexto de club.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal precioEfectivo;
    /**
     * Override persistido en {@code club_productos.precio_venta} (solo GLOBAL con contexto club).
     * LOCAL: siempre null/omitido — el precio de venta vive en {@code productos.precio}.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal precioVentaClub;
    private Integer puntosValor;
    private String tipo; // GLOBAL | LOCAL (origen)
    /** true si el producto es un Combo (habilita registro de asistencia). Independiente de tipo. */
    private Boolean esCombo;
    private String estadoAprobacion; // APROBADO | PENDIENTE | RECHAZADO
    /** Solo ADMIN/ANFITRION. Omitido en JSON público (socio). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String comentarioRevision;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer revisadoPorUsuarioId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String revisadoPorNombre;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime revisadoAt;
    private Boolean activo;
    /** Disponible en el club (tabla club_productos). Solo se rellena cuando se lista por club para anfitrión/admin. */
    private Boolean disponible;
    /**
     * Definición de grupos/opciones.
     * GET público (SOCIO): siempre lista (vacía si el producto no tiene grupos);
     * dentro de cada grupo solo opciones con activo=true.
     * GET interno (ADMIN/ANFITRION): definición completa, incluidas inactivas.
     * PUT: null = preservar; [] = borrar todos; con contenido = reemplazar definición completa.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ProductoGrupoOpcionDTO> gruposOpciones;
    private LocalDateTime createdAt;
}

