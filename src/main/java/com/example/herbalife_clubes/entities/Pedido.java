package com.example.herbalife_clubes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membresia_id", nullable = true)
    private Membresia membresia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_consumo", nullable = false, length = 30)
    private TipoConsumo tipoConsumo;

    @Column(name = "tiempo_estimado_minutos")
    private Integer tiempoEstimadoMinutos;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPedido estado;

    @Column(name = "fecha_pedido")
    private LocalDateTime fechaPedido;

    @Column(name = "tipo_pago", length = 30)
    private String tipoPago;

    // Campos para compatibilidad con estructura de BD existente
    // Estos campos se llenan automáticamente desde el primer item
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = true)
    private Integer cantidad;

    /** UUID v4 generado por el cliente para idempotencia de POST /pedidos/con-items. */
    @Column(name = "client_order_id", length = 36)
    private String clientOrderId;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoItem> items = new ArrayList<>();

    /** Líneas comerciales de combos (precio congelado). LAZY + batch: no JOIN FETCH con items. */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<PedidoCombo> pedidoCombos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (fechaPedido == null) {
            fechaPedido = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoPedido.RECIBIDO;
        }
        if (tipoConsumo == null) {
            tipoConsumo = TipoConsumo.EN_LUGAR;
        }
        // Poblar producto_id y cantidad desde el primer item para compatibilidad con BD
        if (items != null && !items.isEmpty()) {
            PedidoItem firstItem = items.get(0);
            if (firstItem.getProducto() != null) {
                this.producto = firstItem.getProducto();
            }
            if (firstItem.getCantidad() != null) {
                this.cantidad = firstItem.getCantidad();
            }
        }
    }
}

