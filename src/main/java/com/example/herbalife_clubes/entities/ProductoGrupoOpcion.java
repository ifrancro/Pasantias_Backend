package com.example.herbalife_clubes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * Grupo de opciones de un producto (p. ej. Sabores, Consistencia).
 * Cascade ALL + orphanRemoval: la definición se reemplaza completa en PUT
 * porque aún no existen pedido_item_opciones ni club_producto_opciones.
 * Deuda futura: esas FKs exigirán sincronización por IDs, no reemplazo ciego.
 */
@Entity
@Table(name = "producto_grupos_opciones",
        uniqueConstraints = @UniqueConstraint(name = "uq_pgo_producto_nombre",
                columnNames = {"producto_id", "nombre"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"producto", "opciones"})
@ToString(exclude = {"producto", "opciones"})
public class ProductoGrupoOpcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "min_selecciones", nullable = false)
    private Integer minSelecciones;

    @Column(name = "max_selecciones")
    private Integer maxSelecciones;

    @Column(name = "permite_repetir", nullable = false)
    private Boolean permiteRepetir;

    /**
     * LAZY + batch: no va en el EntityGraph junto con gruposOpciones
     * (MultipleBagFetchException). Se inicializa al mapear dentro de la TX.
     */
    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orden ASC, id ASC")
    @BatchSize(size = 50)
    private List<ProductoOpcion> opciones = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (orden == null) {
            orden = 0;
        }
        if (minSelecciones == null) {
            minSelecciones = 0;
        }
        if (permiteRepetir == null) {
            permiteRepetir = false;
        }
    }
}
