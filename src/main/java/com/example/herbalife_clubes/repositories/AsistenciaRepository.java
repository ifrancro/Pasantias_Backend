package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {
    Optional<Asistencia> findByMembresiaIdAndClubIdAndFechaDia(Integer membresiaId, Integer clubId, LocalDate fechaDia);
    Optional<Asistencia> findByMembresiaIdAndFechaDia(Integer membresiaId, LocalDate fechaDia);
    List<Asistencia> findByMembresiaId(Integer membresiaId);
    List<Asistencia> findByClubId(Integer clubId);

    @Query("SELECT a.fechaDia, COUNT(a) FROM Asistencia a WHERE a.club.id = :clubId "
            + "AND a.fechaDia >= :ini AND a.fechaDia <= :fin GROUP BY a.fechaDia ORDER BY a.fechaDia")
    List<Object[]> countAsistenciasPorDiaEnClub(
            @Param("clubId") Integer clubId,
            @Param("ini") LocalDate ini,
            @Param("fin") LocalDate fin);
}

