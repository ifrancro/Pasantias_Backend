package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}

