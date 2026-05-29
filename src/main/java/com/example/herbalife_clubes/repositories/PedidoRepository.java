package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByMembresiaId(Integer membresiaId);
    List<Pedido> findByClubId(Integer clubId);
    
    /**
     * Obtiene todos los pedidos de un club con todas las relaciones cargadas (JOIN FETCH)
     * para evitar problemas de Lazy Loading.
     * Carga: Club, Membresia, Items, Producto (de cada item)
     */
    @Query("SELECT DISTINCT p FROM Pedido p " +
           "LEFT JOIN FETCH p.club c " +
           "LEFT JOIN FETCH c.anfitrion " +
           "LEFT JOIN FETCH p.membresia m " +
           "LEFT JOIN FETCH p.items i " +
           "LEFT JOIN FETCH i.producto " +
           "WHERE p.club.id = :clubId " +
           "ORDER BY p.fechaPedido DESC")
    List<Pedido> findByClubIdWithRelations(@Param("clubId") Integer clubId);
    
    /**
     * Obtiene todos los pedidos de un socio con todas las relaciones cargadas (JOIN FETCH)
     * para evitar problemas de Lazy Loading.
     * Carga: Club, Membresia, Items, Producto (de cada item)
     */
    @Query("SELECT DISTINCT p FROM Pedido p " +
           "LEFT JOIN FETCH p.club c " +
           "LEFT JOIN FETCH p.membresia m " +
           "LEFT JOIN FETCH p.items i " +
           "LEFT JOIN FETCH i.producto " +
           "WHERE p.membresia.id = :membresiaId " +
           "ORDER BY p.fechaPedido DESC")
    List<Pedido> findByMembresiaIdWithRelations(@Param("membresiaId") Integer membresiaId);

    @Query(value = """
            SELECT COALESCE(SUM(COALESCE(pi.subtotal, COALESCE(pi.precio_unitario, 0) * COALESCE(pi.cantidad, 1))), 0)
            FROM pedido_items pi
            INNER JOIN pedidos p ON p.id = pi.pedido_id
            WHERE p.club_id = :clubId
              AND p.fecha_pedido >= :desde
              AND p.fecha_pedido < :hasta
              AND p.estado = 'ENTREGADO'
            """, nativeQuery = true)
    BigDecimal sumIngresosPuntosValorEntregados(
            @Param("clubId") Integer clubId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query(value = """
            SELECT CAST(p.fecha_pedido AS DATE) AS dia, COUNT(*) AS cnt
            FROM pedidos p
            WHERE p.club_id = :clubId
              AND p.fecha_pedido >= :desde
              AND p.fecha_pedido < :hasta
              AND p.estado <> 'CANCELADO'
            GROUP BY CAST(p.fecha_pedido AS DATE)
            ORDER BY dia
            """, nativeQuery = true)
    List<Object[]> countPedidosPorDia(
            @Param("clubId") Integer clubId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query(value = """
            SELECT pr.id, pr.nombre, SUM(pi.cantidad) AS total_vendido
            FROM pedido_items pi
            INNER JOIN pedidos p ON p.id = pi.pedido_id
            INNER JOIN productos pr ON pr.id = pi.producto_id
            WHERE p.club_id = :clubId
              AND p.fecha_pedido >= :desde
              AND p.fecha_pedido < :hasta
              AND p.estado = 'ENTREGADO'
            GROUP BY pr.id, pr.nombre
            ORDER BY total_vendido DESC
            """, nativeQuery = true)
    List<Object[]> rankingProductosVendidos(
            @Param("clubId") Integer clubId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}

