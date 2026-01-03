package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {
    Optional<Asistencia> findByMembresiaIdAndClubIdAndFechaDia(Integer membresiaId, Integer clubId, LocalDate fechaDia);
    List<Asistencia> findByMembresiaId(Integer membresiaId);
    List<Asistencia> findByClubId(Integer clubId);
}

