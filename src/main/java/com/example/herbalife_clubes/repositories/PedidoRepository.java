package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.EstadoPedido;
import com.example.herbalife_clubes.entities.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Carga: Club, anfitrion, Membresia, Items, Producto y Combo (de cada item).
     * No incluir {@code items.opciones}: segunda bag → MultipleBagFetchException.
     * {@code opciones} se cargan LAZY + {@code @BatchSize} al mapear dentro de la TX.
     */
    @Query("SELECT DISTINCT p FROM Pedido p " +
           "LEFT JOIN FETCH p.club c " +
           "LEFT JOIN FETCH c.anfitrion " +
           "LEFT JOIN FETCH p.membresia m " +
           "LEFT JOIN FETCH p.items i " +
           "LEFT JOIN FETCH i.producto " +
           "LEFT JOIN FETCH i.combo " +
           "WHERE p.club.id = :clubId " +
           "ORDER BY p.fechaPedido DESC")
    List<Pedido> findByClubIdWithRelations(@Param("clubId") Integer clubId);
    
    /**
     * Obtiene todos los pedidos de un socio con todas las relaciones cargadas (JOIN FETCH)
     * para evitar problemas de Lazy Loading.
     * Carga: Club, Membresia, Items, Producto y Combo (de cada item).
     * No incluir {@code items.opciones}: segunda bag → MultipleBagFetchException.
     * {@code opciones} se cargan LAZY + {@code @BatchSize} al mapear dentro de la TX.
     */
    @Query("SELECT DISTINCT p FROM Pedido p " +
           "LEFT JOIN FETCH p.club c " +
           "LEFT JOIN FETCH p.membresia m " +
           "LEFT JOIN FETCH p.items i " +
           "LEFT JOIN FETCH i.producto " +
           "LEFT JOIN FETCH i.combo " +
           "WHERE p.membresia.id = :membresiaId " +
           "ORDER BY p.fechaPedido DESC")
    List<Pedido> findByMembresiaIdWithRelations(@Param("membresiaId") Integer membresiaId);

    @Query(value = """
            SELECT COALESCE(SUM(ingreso), 0) FROM (
                SELECT COALESCE(pi.subtotal, COALESCE(pi.precio_unitario, 0) * COALESCE(pi.cantidad, 1)) AS ingreso
                FROM pedido_items pi
                INNER JOIN pedidos p ON p.id = pi.pedido_id
                WHERE p.club_id = :clubId
                  AND p.fecha_pedido >= :desde
                  AND p.fecha_pedido < :hasta
                  AND p.estado = 'ENTREGADO'
                  AND pi.pedido_combo_id IS NULL
                UNION ALL
                SELECT pc.subtotal_snapshot AS ingreso
                FROM pedido_combos pc
                INNER JOIN pedidos p ON p.id = pc.pedido_id
                WHERE p.club_id = :clubId
                  AND p.fecha_pedido >= :desde
                  AND p.fecha_pedido < :hasta
                  AND p.estado = 'ENTREGADO'
            ) ingresos
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

    @Query(value = """
            SELECT CAST(p.fecha_pedido AS DATE) AS dia, COUNT(*) AS cnt
            FROM pedidos p
            WHERE p.club_id = :clubId
              AND p.fecha_pedido >= :desde
              AND p.fecha_pedido < :hasta
              AND p.estado = 'ENTREGADO'
            GROUP BY CAST(p.fecha_pedido AS DATE)
            ORDER BY dia
            """, nativeQuery = true)
    List<Object[]> countEntregadosPorDia(
            @Param("clubId") Integer clubId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query(value = """
            SELECT dia, COALESCE(SUM(ingreso), 0) FROM (
                SELECT CAST(p.fecha_pedido AS DATE) AS dia,
                       COALESCE(pi.subtotal, COALESCE(pi.precio_unitario, 0) * COALESCE(pi.cantidad, 1)) AS ingreso
                FROM pedido_items pi
                INNER JOIN pedidos p ON p.id = pi.pedido_id
                WHERE p.club_id = :clubId
                  AND p.fecha_pedido >= :desde
                  AND p.fecha_pedido < :hasta
                  AND p.estado = 'ENTREGADO'
                  AND pi.pedido_combo_id IS NULL
                UNION ALL
                SELECT CAST(p.fecha_pedido AS DATE) AS dia, pc.subtotal_snapshot AS ingreso
                FROM pedido_combos pc
                INNER JOIN pedidos p ON p.id = pc.pedido_id
                WHERE p.club_id = :clubId
                  AND p.fecha_pedido >= :desde
                  AND p.fecha_pedido < :hasta
                  AND p.estado = 'ENTREGADO'
            ) ingresos_por_linea
            GROUP BY dia
            ORDER BY dia
            """, nativeQuery = true)
    List<Object[]> ingresosEntregadosPorDia(
            @Param("clubId") Integer clubId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT p.id FROM Pedido p " +
           "WHERE p.club.id = :clubId " +
           "AND p.estado = :estadoEntregado " +
           "AND p.fechaPedido >= :desde " +
           "AND p.fechaPedido < :hasta " +
           "ORDER BY p.fechaPedido ASC")
    List<Integer> findEntregadoIdsByClubAndRango(
            @Param("clubId") Integer clubId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("estadoEntregado") EstadoPedido estadoEntregado);

    @Query("SELECT DISTINCT p FROM Pedido p " +
           "LEFT JOIN FETCH p.club " +
           "LEFT JOIN FETCH p.producto " +
           "LEFT JOIN FETCH p.membresia m " +
           "LEFT JOIN FETCH m.usuario " +
           "LEFT JOIN FETCH m.referidoPorMembresia " +
           "LEFT JOIN FETCH p.items i " +
           "LEFT JOIN FETCH i.producto " +
           "WHERE p.id IN :ids " +
           "ORDER BY p.fechaPedido ASC")
    List<Pedido> findEntregadosDetalleByIds(@Param("ids") List<Integer> ids);

    @Query("SELECT COUNT(p) FROM Pedido p " +
           "WHERE p.membresia.id = :membresiaId " +
           "AND p.club.id = :clubId " +
           "AND p.estado = :estadoEntregado " +
           "AND p.fechaPedido < :antes")
    long countEntregadosAntesDe(
            @Param("membresiaId") Integer membresiaId,
            @Param("clubId") Integer clubId,
            @Param("antes") LocalDateTime antes,
            @Param("estadoEntregado") EstadoPedido estadoEntregado);

    /**
     * Página de IDs (sin JOIN FETCH de colecciones) — paginación real en BD.
     * Orden estable: fechaPedido DESC, id DESC.
     * Filtros opcionales vía flags booleanos: no usar {@code :param IS NULL} sobre
     * enum/timestamp (PostgreSQL no puede inferir el tipo del parámetro).
     */
    @Query(
            value = "SELECT p.id FROM Pedido p WHERE p.club.id = :clubId "
                    + "AND (:estadoPresente = false OR p.estado = :estado) "
                    + "AND (:desdePresente = false OR p.fechaPedido >= :desde) "
                    + "AND (:hastaPresente = false OR p.fechaPedido < :hasta)",
            countQuery = "SELECT COUNT(p) FROM Pedido p WHERE p.club.id = :clubId "
                    + "AND (:estadoPresente = false OR p.estado = :estado) "
                    + "AND (:desdePresente = false OR p.fechaPedido >= :desde) "
                    + "AND (:hastaPresente = false OR p.fechaPedido < :hasta)"
    )
    Page<Integer> findIdsByClubId(
            @Param("clubId") Integer clubId,
            @Param("estadoPresente") boolean estadoPresente,
            @Param("estado") EstadoPedido estado,
            @Param("desdePresente") boolean desdePresente,
            @Param("desde") LocalDateTime desde,
            @Param("hastaPresente") boolean hastaPresente,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    @Query(
            value = "SELECT p.id FROM Pedido p WHERE p.membresia.id = :membresiaId "
                    + "AND (:estadoPresente = false OR p.estado = :estado) "
                    + "AND (:desdePresente = false OR p.fechaPedido >= :desde) "
                    + "AND (:hastaPresente = false OR p.fechaPedido < :hasta)",
            countQuery = "SELECT COUNT(p) FROM Pedido p WHERE p.membresia.id = :membresiaId "
                    + "AND (:estadoPresente = false OR p.estado = :estado) "
                    + "AND (:desdePresente = false OR p.fechaPedido >= :desde) "
                    + "AND (:hastaPresente = false OR p.fechaPedido < :hasta)"
    )
    Page<Integer> findIdsByMembresiaId(
            @Param("membresiaId") Integer membresiaId,
            @Param("estadoPresente") boolean estadoPresente,
            @Param("estado") EstadoPedido estado,
            @Param("desdePresente") boolean desdePresente,
            @Param("desde") LocalDateTime desde,
            @Param("hastaPresente") boolean hastaPresente,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    /**
     * Carga batch de relaciones para una página de IDs (sin Pageable → sin HHH90003004).
     * Incluye combo del item (id/nombre) sin fetchear combo.items.
     */
    @Query("SELECT DISTINCT p FROM Pedido p "
            + "LEFT JOIN FETCH p.club c "
            + "LEFT JOIN FETCH c.anfitrion "
            + "LEFT JOIN FETCH p.membresia m "
            + "LEFT JOIN FETCH p.items i "
            + "LEFT JOIN FETCH i.producto "
            + "LEFT JOIN FETCH i.combo "
            + "WHERE p.id IN :ids")
    List<Pedido> findWithRelationsByIds(@Param("ids") List<Integer> ids);
}

