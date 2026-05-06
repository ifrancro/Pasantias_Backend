package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.MisionProspecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MisionProspectoRepository extends JpaRepository<MisionProspecto, Integer> {
    List<MisionProspecto> findByProspectoIdOrderByIdAsc(Integer prospectoId);
}
