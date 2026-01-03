package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Consumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsumoRepository extends JpaRepository<Consumo, Integer> {
    List<Consumo> findByMembresiaId(Integer membresiaId);
    List<Consumo> findByClubId(Integer clubId);
}

