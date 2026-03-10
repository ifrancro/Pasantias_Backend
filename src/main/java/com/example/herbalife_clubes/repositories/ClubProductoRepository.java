package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.ClubProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubProductoRepository extends JpaRepository<ClubProducto, Integer> {
    List<ClubProducto> findByClubId(Integer clubId);
    List<ClubProducto> findByClubIdAndDisponibleTrue(Integer clubId);
    List<ClubProducto> findByClubIdAndDisponibleTrueAndProducto_Tipo(Integer clubId, String tipo);
    Optional<ClubProducto> findByClubIdAndProductoId(Integer clubId, Integer productoId);

    /**
     * Para la vista del socio: solo productos con registro en club_productos,
     * disponible = true y estado_aprobacion = APROBADO (válido para globales y locales).
     */
    @Query("SELECT cp FROM ClubProducto cp JOIN cp.producto p WHERE cp.club.id = :clubId AND cp.disponible = true AND UPPER(p.estadoAprobacion) = 'APROBADO'")
    List<ClubProducto> findByClubIdAndDisponibleTrueAndProductoAprobado(@Param("clubId") Integer clubId);

    /**
     * Igual que el anterior pero además filtrado por tipo (GLOBAL/LOCAL).
     */
    @Query("SELECT cp FROM ClubProducto cp JOIN cp.producto p WHERE cp.club.id = :clubId AND cp.disponible = true AND UPPER(p.estadoAprobacion) = 'APROBADO' AND UPPER(p.tipo) = :tipo")
    List<ClubProducto> findByClubIdAndDisponibleTrueAndProductoAprobadoAndProductoTipo(@Param("clubId") Integer clubId, @Param("tipo") String tipo);
}


