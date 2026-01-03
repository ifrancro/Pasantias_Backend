package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.NivelSocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NivelSocioRepository extends JpaRepository<NivelSocio, Integer> {
}

