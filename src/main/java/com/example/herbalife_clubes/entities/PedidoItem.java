package com.example.herbalife_clubes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"pedido", "producto", "combo", "opciones"})
@ToString(exclude = {"pedido", "producto", "combo", "opciones"})
public class PedidoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id")
    private Combo combo;

    @Column(name = "nota", columnDefinition = "TEXT")
    private String nota;

    @Column(name = "precio_unitario", precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * Selecciones congeladas del ítem. LAZY + batch: no JOIN FETCH junto con Pedido.items
     * (MultipleBagFetchException).
     */
    @OneToMany(mappedBy = "pedidoItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("grupoOrdenSnapshot ASC, opcionOrdenSnapshot ASC, id ASC")
    @BatchSize(size = 50)
    private List<PedidoItemOpcion> opciones = new ArrayList<>();

    @PrePersist
    @PreUpdate
    protected void calcularSubtotal() {
        if (precioUnitario == null) {
            precioUnitario = BigDecimal.ZERO;
        }
        if (cantidad == null) {
            subtotal = BigDecimal.ZERO;
            return;
        }
        subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}


