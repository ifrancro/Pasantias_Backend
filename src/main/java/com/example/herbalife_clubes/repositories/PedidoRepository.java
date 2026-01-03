package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByMembresiaId(Integer membresiaId);
    List<Pedido> findByClubId(Integer clubId);
}

