package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.CompraManual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompraManualRepository extends JpaRepository<CompraManual, Integer> {
    List<CompraManual> findByMembresiaIdOrderByFechaDescIdDesc(Integer membresiaId);
}
