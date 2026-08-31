package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Membresia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembresiaRepository extends JpaRepository<Membresia, Integer> {

    @Override
    @EntityGraph(attributePaths = {"usuario", "club", "nivel", "referidoPorMembresia.usuario"})
    Optional<Membresia> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = {"usuario", "club", "nivel", "referidoPorMembresia.usuario"})
    List<Membresia> findAll();

    @EntityGraph(attributePaths = {"usuario", "club", "nivel", "referidoPorMembresia.usuario"})
    Optional<Membresia> findByUsuarioId(Integer usuarioId);

    @EntityGraph(attributePaths = {"usuario", "club", "nivel", "referidoPorMembresia.usuario"})
    List<Membresia> findByClubId(Integer clubId);

    boolean existsByUsuarioId(Integer usuarioId);

    @EntityGraph(attributePaths = {"usuario", "club", "nivel", "referidoPorMembresia.usuario"})
    Optional<Membresia> findByNumeroSocio(String numeroSocio);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Membresia m WHERE m.id = :id")
    Optional<Membresia> findByIdForUpdate(@Param("id") Integer id);

    @EntityGraph(attributePaths = {"usuario", "club", "nivel", "referidoPorMembresia.usuario"})
    List<Membresia> findByReferidoPorMembresiaId(Integer referidoPorMembresiaId);

    @Query(value = "SELECT m FROM Membresia m JOIN FETCH m.usuario u JOIN FETCH m.club c " +
           "WHERE m.estado = 'ACTIVA' AND (" +
           "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.apellido) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.numeroSocio) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY u.nombre",
           countQuery = "SELECT COUNT(m) FROM Membresia m JOIN m.usuario u " +
           "WHERE m.estado = 'ACTIVA' AND (" +
           "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.apellido) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.numeroSocio) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Membresia> buscarMiembrosGlobal(@Param("query") String query, Pageable pageable);

    @Query("SELECT m FROM Membresia m JOIN FETCH m.usuario u JOIN FETCH m.club c " +
           "WHERE m.estado = 'ACTIVA' ORDER BY u.nombre")
    List<Membresia> findAllActiveWithUsuario(Pageable pageable);

    /**
     * Página de IDs de membresías de un club (paginación en BD).
     * Orden estable: fechaRegistro DESC, id DESC.
     */
    @Query(
            value = "SELECT m.id FROM Membresia m JOIN m.usuario u WHERE m.club.id = :clubId "
                    + "AND (:q IS NULL OR :q = '' OR "
                    + "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR "
                    + "LOWER(u.apellido) LIKE LOWER(CONCAT('%', :q, '%')) OR "
                    + "LOWER(m.numeroSocio) LIKE LOWER(CONCAT('%', :q, '%')))",
            countQuery = "SELECT COUNT(m) FROM Membresia m JOIN m.usuario u WHERE m.club.id = :clubId "
                    + "AND (:q IS NULL OR :q = '' OR "
                    + "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR "
                    + "LOWER(u.apellido) LIKE LOWER(CONCAT('%', :q, '%')) OR "
                    + "LOWER(m.numeroSocio) LIKE LOWER(CONCAT('%', :q, '%')))"
    )
    Page<Integer> findIdsByClubId(
            @Param("clubId") Integer clubId,
            @Param("q") String q,
            Pageable pageable);

    @Query(
            value = "SELECT m.id FROM Membresia m JOIN m.usuario u "
                    + "WHERE m.estado = 'ACTIVA' AND ("
                    + "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR "
                    + "LOWER(u.apellido) LIKE LOWER(CONCAT('%', :query, '%')) OR "
                    + "LOWER(m.numeroSocio) LIKE LOWER(CONCAT('%', :query, '%')))",
            countQuery = "SELECT COUNT(m) FROM Membresia m JOIN m.usuario u "
                    + "WHERE m.estado = 'ACTIVA' AND ("
                    + "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR "
                    + "LOWER(u.apellido) LIKE LOWER(CONCAT('%', :query, '%')) OR "
                    + "LOWER(m.numeroSocio) LIKE LOWER(CONCAT('%', :query, '%')))"
    )
    Page<Integer> buscarIdsGlobal(@Param("query") String query, Pageable pageable);

    @Query(
            value = "SELECT m.id FROM Membresia m JOIN m.usuario u WHERE m.estado = 'ACTIVA'",
            countQuery = "SELECT COUNT(m) FROM Membresia m WHERE m.estado = 'ACTIVA'"
    )
    Page<Integer> findIdsAllActive(Pageable pageable);

    // LEFT JOIN FETCH m.nivel porque nivel_id es nullable — un JOIN FETCH normal descartaría filas sin nivel
    @Query("SELECT DISTINCT m FROM Membresia m "
            + "JOIN FETCH m.usuario "
            + "JOIN FETCH m.club "
            + "LEFT JOIN FETCH m.nivel "
            + "WHERE m.id IN :ids")
    List<Membresia> findWithUsuarioClubByIds(@Param("ids") List<Integer> ids);
}
