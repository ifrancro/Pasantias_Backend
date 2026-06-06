package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.ProductoSabor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoSaborRepository extends JpaRepository<ProductoSabor, Integer> {
    List<ProductoSabor> findByProductoId(Integer productoId);
    Optional<ProductoSabor> findByProductoIdAndSaborId(Integer productoId, Integer saborId);
    boolean existsByProductoIdAndSaborId(Integer productoId, Integer saborId);
    void deleteByProductoIdAndSaborId(Integer productoId, Integer saborId);
}
