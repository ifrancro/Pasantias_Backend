package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Sabor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SaborRepository extends JpaRepository<Sabor, Integer> {
    List<Sabor> findByHubIdAndActivoTrue(Integer hubId);
    List<Sabor> findByHubId(Integer hubId);
    Optional<Sabor> findByHubIdAndNombreIgnoreCase(Integer hubId, String nombre);
    boolean existsByHubIdAndNombreIgnoreCase(Integer hubId, String nombre);
}
