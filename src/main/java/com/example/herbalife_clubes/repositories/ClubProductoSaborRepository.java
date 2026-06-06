package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.ClubProductoSabor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClubProductoSaborRepository extends JpaRepository<ClubProductoSabor, Integer> {
    List<ClubProductoSabor> findByClubIdAndProductoId(Integer clubId, Integer productoId);
    Optional<ClubProductoSabor> findByClubIdAndProductoIdAndSaborId(Integer clubId, Integer productoId, Integer saborId);
    boolean existsByClubIdAndProductoIdAndSaborId(Integer clubId, Integer productoId, Integer saborId);
}
