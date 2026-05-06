package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Prospecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProspectoRepository extends JpaRepository<Prospecto, Integer> {
    List<Prospecto> findByClubIdOrderByFechaCreacionDescIdDesc(Integer clubId);
}
