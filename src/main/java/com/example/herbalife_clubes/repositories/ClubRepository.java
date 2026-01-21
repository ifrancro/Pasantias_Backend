package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubRepository extends JpaRepository<Club, Integer> {
    List<Club> findByHubId(Integer hubId);
    List<Club> findByAnfitrionId(Integer anfitrionId);
    List<Club> findByEstado(String estado);
    Optional<Club> findByIdAndEstado(Integer id, String estado);
    List<Club> findByEstadoIn(List<String> estados);
    Optional<Club> findByIdAndEstadoIn(Integer id, List<String> estados);
    
    // Consulta personalizada para asegurar que funcione correctamente
    // Compara directamente el anfitrion_id (columna de BD) con el ID del anfitrión
    @Query("SELECT c FROM Club c WHERE c.id = :clubId AND c.anfitrion.id = :anfitrionId")
    Optional<Club> findByIdAndAnfitrionId(@Param("clubId") Integer clubId, @Param("anfitrionId") Integer anfitrionId);
}

