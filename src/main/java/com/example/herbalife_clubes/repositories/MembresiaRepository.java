package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Membresia;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembresiaRepository extends JpaRepository<Membresia, Integer> {
    Optional<Membresia> findByUsuarioId(Integer usuarioId);
    List<Membresia> findByClubId(Integer clubId);
    boolean existsByUsuarioId(Integer usuarioId);
    Optional<Membresia> findByNumeroSocio(String numeroSocio);
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
}

