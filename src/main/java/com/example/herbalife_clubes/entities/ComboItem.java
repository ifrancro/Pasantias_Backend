package com.example.herbalife_clubes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "combo_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"combo_id", "producto_id", "sabor_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComboItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id", nullable = false)
    private Combo combo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sabor_id", nullable = true)
    private Sabor sabor;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @PrePersist
    protected void onCreate() {
        if (cantidad == null) {
            cantidad = 1;
        }
    }
}
