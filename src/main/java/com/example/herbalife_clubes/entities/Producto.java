package com.example.herbalife_clubes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_creador_id", nullable = true)
    private Club clubCreador;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "imagen_url", length = 1024)
    private String imagenUrl;

    @Column(name = "ingredientes", columnDefinition = "TEXT")
    private String ingredientes;

    @Column(name = "puntos_valor")
    private Integer puntosValor;

    @Column(name = "precio", precision = 10, scale = 2)
    private BigDecimal precio;

    /** Origen del producto: GLOBAL (del hub) o LOCAL (creado por un club). */
    @Column(name = "tipo", length = 50)
    private String tipo;

    /**
     * Marca el producto como Combo (paquete consumible que habilita el registro de asistencia).
     * Independiente de {@link #tipo}: un combo puede ser GLOBAL o LOCAL.
     */
    @Column(name = "es_combo")
    private Boolean esCombo;

    @Column(name = "estado_aprobacion", length = 50)
    private String estadoAprobacion;

    @Column(name = "comentario_revision", columnDefinition = "TEXT")
    private String comentarioRevision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por_usuario_id")
    private Usuario revisadoPor;

    @Column(name = "revisado_at")
    private LocalDateTime revisadoAt;

    @Column(name = "activo")
    private Boolean activo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Definición estructural de grupos/opciones. Cascade + orphanRemoval:
     * PUT con gruposOpciones != null reemplaza la colección completa.
     * Seguro en este ticket porque no hay pedido_item_opciones ni club_producto_opciones.
     * Deuda futura: antes de introducir esas FKs hay que dejar de regenerar IDs
     * y sincronizar por identidad estable.
     * Fetch: EntityGraph JOIN FETCH de esta bag sola (no de opciones a la vez)
     * para evitar MultipleBagFetchException. Orden semántico por {@code orden}.
     */
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, id ASC")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<ProductoGrupoOpcion> gruposOpciones = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (activo == null) {
            activo = true;
        }
        if (puntosValor == null) {
            puntosValor = 0;
        }
        if (precio == null) {
            precio = BigDecimal.ZERO;
        }
        if (esCombo == null) {
            esCombo = false;
        }
        createdAt = LocalDateTime.now();
    }
}

