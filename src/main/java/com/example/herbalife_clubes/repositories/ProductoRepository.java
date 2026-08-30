package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    /**
     * To-one + a lo sumo la bag {@code gruposOpciones}.
     * No incluir {@code gruposOpciones.opciones}: Hibernate lanza
     * {@code MultipleBagFetchException} al JOIN FETCH simultáneo de dos {@code List}.
     * {@code opciones} se carga LAZY + {@code @BatchSize} al mapear dentro de la TX.
     * DISTINCT evita raíces duplicadas por el JOIN FETCH de grupos.
     */
    @Override
    @EntityGraph(attributePaths = {
            "hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor", "gruposOpciones"
    })
    @Query("select distinct p from Producto p")
    List<Producto> findAll();

    @Override
    @EntityGraph(attributePaths = {
            "hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor", "gruposOpciones"
    })
    Optional<Producto> findById(Integer id);

    @EntityGraph(attributePaths = {
            "hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor", "gruposOpciones"
    })
    @Query("select distinct p from Producto p where p.hub.id = :hubId")
    List<Producto> findByHubId(@Param("hubId") Integer hubId);

    @EntityGraph(attributePaths = {
            "hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor", "gruposOpciones"
    })
    @Query("select distinct p from Producto p where p.hub.id = :hubId and p.activo = true")
    List<Producto> findByHubIdAndActivoTrue(@Param("hubId") Integer hubId);

    @EntityGraph(attributePaths = {
            "hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor", "gruposOpciones"
    })
    @Query("select distinct p from Producto p where p.clubCreador.id = :clubCreadorId")
    List<Producto> findByClubCreadorId(@Param("clubCreadorId") Integer clubCreadorId);

    @EntityGraph(attributePaths = {
            "hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor", "gruposOpciones"
    })
    @Query("select distinct p from Producto p where p.estadoAprobacion = :estadoAprobacion")
    List<Producto> findByEstadoAprobacion(@Param("estadoAprobacion") String estadoAprobacion);

    @EntityGraph(attributePaths = {
            "hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor", "gruposOpciones"
    })
    @Query("select distinct p from Producto p where p.estadoAprobacion <> :estadoAprobacion")
    List<Producto> findByEstadoAprobacionNot(@Param("estadoAprobacion") String estadoAprobacion);

    @EntityGraph(attributePaths = {
            "hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor", "gruposOpciones"
    })
    @Query("select distinct p from Producto p where p.estadoAprobacion = :estadoAprobacion "
            + "and p.clubCreador.id = :clubCreadorId")
    List<Producto> findByEstadoAprobacionAndClubCreadorId(
            @Param("estadoAprobacion") String estadoAprobacion,
            @Param("clubCreadorId") Integer clubCreadorId);

    @EntityGraph(attributePaths = {
            "hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor", "gruposOpciones"
    })
    @Query("select distinct p from Producto p where p.hub.id = :hubId "
            + "and p.tipo = :tipo and p.estadoAprobacion = :estadoAprobacion")
    List<Producto> findByHubIdAndTipoAndEstadoAprobacion(
            @Param("hubId") Integer hubId,
            @Param("tipo") String tipo,
            @Param("estadoAprobacion") String estadoAprobacion);

    @EntityGraph(attributePaths = {
            "hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor", "gruposOpciones"
    })
    @Query("select distinct p from Producto p where p.clubCreador.id = :clubCreadorId "
            + "and p.tipo = :tipo and p.estadoAprobacion = :estadoAprobacion")
    List<Producto> findByClubCreadorIdAndTipoAndEstadoAprobacion(
            @Param("clubCreadorId") Integer clubCreadorId,
            @Param("tipo") String tipo,
            @Param("estadoAprobacion") String estadoAprobacion);
}
