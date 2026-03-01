package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    List<Producto> findByHubId(Integer hubId);
    List<Producto> findByHubIdAndActivoTrue(Integer hubId);
    List<Producto> findByClubCreadorId(Integer clubCreadorId);
    List<Producto> findByEstadoAprobacion(String estadoAprobacion);
    List<Producto> findByEstadoAprobacionNot(String estadoAprobacion); // Para excluir PENDIENTE
}

