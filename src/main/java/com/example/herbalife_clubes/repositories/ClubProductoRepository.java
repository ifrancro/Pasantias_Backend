package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.ClubProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubProductoRepository extends JpaRepository<ClubProducto, Integer> {
    List<ClubProducto> findByClubId(Integer clubId);
    List<ClubProducto> findByClubIdAndDisponibleTrue(Integer clubId);
    Optional<ClubProducto> findByClubIdAndProductoId(Integer clubId, Integer productoId);
}


