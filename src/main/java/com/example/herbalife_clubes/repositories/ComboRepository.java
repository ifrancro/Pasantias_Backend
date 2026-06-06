package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Combo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComboRepository extends JpaRepository<Combo, Integer> {
    List<Combo> findByClubId(Integer clubId);
    List<Combo> findByClubIdAndActivoTrue(Integer clubId);
}
