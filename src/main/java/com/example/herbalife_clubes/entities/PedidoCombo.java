package com.example.herbalife_clubes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido_combos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"pedido", "combo", "items"})
@ToString(exclude = {"pedido", "combo", "items"})
public class PedidoCombo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id")
    private Combo combo;

    @Column(name = "combo_nombre_snapshot", nullable = false, length = 150)
    private String comboNombreSnapshot;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitarioSnapshot;

    @Column(name = "subtotal_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalSnapshot;

    @Column(name = "puntos_valor_snapshot", nullable = false)
    private Integer puntosValorSnapshot;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "pedidoCombo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<PedidoItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (puntosValorSnapshot == null) {
            puntosValorSnapshot = 0;
        }
    }
}
