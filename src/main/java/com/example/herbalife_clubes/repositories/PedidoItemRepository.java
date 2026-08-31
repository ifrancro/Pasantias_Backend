package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.EstadoPedido;
import com.example.herbalife_clubes.entities.PedidoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoItemRepository extends JpaRepository<PedidoItem, Integer> {
    List<PedidoItem> findByPedidoId(Integer pedidoId);
    Optional<PedidoItem> findByPedidoIdAndProductoId(Integer pedidoId, Integer productoId);

    @Query("SELECT COUNT(pi) FROM PedidoItem pi "
            + "JOIN pi.pedido p "
            + "JOIN pi.producto pr "
            + "WHERE p.membresia.id = :membresiaId "
            + "AND p.estado = :estadoEntregado "
            + "AND pi.pedidoCombo IS NULL "
            + "AND (pr.esCombo = true OR UPPER(pr.tipo) = 'COMBO')")
    long countCombosConsumidosEntregados(
            @Param("membresiaId") Integer membresiaId,
            @Param("estadoEntregado") EstadoPedido estadoEntregado);

    @Query("SELECT COUNT(pi) > 0 FROM PedidoItem pi "
            + "JOIN pi.pedido p "
            + "JOIN pi.producto pr "
            + "WHERE p.membresia.id = :membresiaId "
            + "AND p.estado = :estadoEntregado "
            + "AND pi.pedidoCombo IS NULL "
            + "AND (pr.esCombo = true OR UPPER(pr.tipo) = 'COMBO')")
    boolean hasConsumedComboEntregado(
            @Param("membresiaId") Integer membresiaId,
            @Param("estadoEntregado") EstadoPedido estadoEntregado);

    @Query("SELECT COUNT(pi) > 0 FROM PedidoItem pi "
            + "JOIN pi.pedido p "
            + "JOIN pi.producto pr "
            + "WHERE p.membresia.id = :membresiaId "
            + "AND p.estado = :estadoEntregado "
            + "AND pi.pedidoCombo IS NULL "
            + "AND (pr.esCombo = true OR UPPER(pr.tipo) = 'COMBO') "
            + "AND p.fechaPedido >= :inicioDia "
            + "AND p.fechaPedido < :finDia")
    boolean hasConsumedComboEntregadoHoy(
            @Param("membresiaId") Integer membresiaId,
            @Param("estadoEntregado") EstadoPedido estadoEntregado,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia);

    @Query("SELECT COUNT(pi) FROM PedidoItem pi "
            + "JOIN pi.pedido p "
            + "JOIN pi.producto pr "
            + "WHERE p.membresia.id = :membresiaId "
            + "AND p.estado = :estadoEntregado "
            + "AND pi.pedidoCombo IS NULL "
            + "AND (pr.esCombo = true OR UPPER(pr.tipo) = 'COMBO') "
            + "AND p.fechaPedido >= :inicioDia "
            + "AND p.fechaPedido < :finDia")
    long countCombosConsumidosEntregadosHoy(
            @Param("membresiaId") Integer membresiaId,
            @Param("estadoEntregado") EstadoPedido estadoEntregado,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia);
}

