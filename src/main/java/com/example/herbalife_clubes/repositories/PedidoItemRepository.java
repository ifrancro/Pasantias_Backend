package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.PedidoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoItemRepository extends JpaRepository<PedidoItem, Integer> {
    List<PedidoItem> findByPedidoId(Integer pedidoId);
    Optional<PedidoItem> findByPedidoIdAndProductoId(Integer pedidoId, Integer productoId);
}


