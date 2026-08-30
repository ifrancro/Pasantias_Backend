package com.example.herbalife_clubes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "pedido_item_opciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"pedidoItem", "grupo", "opcion"})
@ToString(exclude = {"pedidoItem", "grupo", "opcion"})
public class PedidoItemOpcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_item_id", nullable = false)
    private PedidoItem pedidoItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private ProductoGrupoOpcion grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_id")
    private ProductoOpcion opcion;

    @Column(name = "grupo_nombre_snapshot", nullable = false, length = 100)
    private String grupoNombreSnapshot;

    @Column(name = "opcion_nombre_snapshot", nullable = false, length = 100)
    private String opcionNombreSnapshot;

    @Column(name = "grupo_orden_snapshot", nullable = false)
    private Integer grupoOrdenSnapshot;

    @Column(name = "opcion_orden_snapshot", nullable = false)
    private Integer opcionOrdenSnapshot;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;
}
