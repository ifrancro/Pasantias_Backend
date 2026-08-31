package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.EstadoPedido;
import com.example.herbalife_clubes.entities.PedidoCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PedidoComboRepository extends JpaRepository<PedidoCombo, Integer> {

    /**
     * Combos modernos V21: cuenta unidades comerciales (pedido_combos.cantidad), no componentes.
     */
    @Query("SELECT COALESCE(SUM(pc.cantidad), 0) FROM PedidoCombo pc "
            + "JOIN pc.pedido p "
            + "WHERE p.membresia.id = :membresiaId "
            + "AND p.estado = :estadoEntregado "
            + "AND p.fechaPedido >= :inicioDia "
            + "AND p.fechaPedido < :finDia")
    long sumCombosEntregadosHoy(
            @Param("membresiaId") Integer membresiaId,
            @Param("estadoEntregado") EstadoPedido estadoEntregado,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia);

    @Query("SELECT COUNT(pc) > 0 FROM PedidoCombo pc "
            + "JOIN pc.pedido p "
            + "WHERE p.membresia.id = :membresiaId "
            + "AND p.estado = :estadoEntregado "
            + "AND p.fechaPedido >= :inicioDia "
            + "AND p.fechaPedido < :finDia")
    boolean hasComboEntregadoHoy(
            @Param("membresiaId") Integer membresiaId,
            @Param("estadoEntregado") EstadoPedido estadoEntregado,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia);
}
