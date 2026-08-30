package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    @Override
    @EntityGraph(attributePaths = {"hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor"})
    List<Producto> findAll();

    @Override
    @EntityGraph(attributePaths = {"hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor"})
    Optional<Producto> findById(Integer id);

    @EntityGraph(attributePaths = {"hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor"})
    List<Producto> findByHubId(Integer hubId);

    @EntityGraph(attributePaths = {"hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor"})
    List<Producto> findByHubIdAndActivoTrue(Integer hubId);

    @EntityGraph(attributePaths = {"hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor"})
    List<Producto> findByClubCreadorId(Integer clubCreadorId);

    @EntityGraph(attributePaths = {"hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor"})
    List<Producto> findByEstadoAprobacion(String estadoAprobacion);

    @EntityGraph(attributePaths = {"hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor"})
    List<Producto> findByEstadoAprobacionNot(String estadoAprobacion); // Para excluir PENDIENTE

    @EntityGraph(attributePaths = {"hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor"})
    List<Producto> findByEstadoAprobacionAndClubCreadorId(String estadoAprobacion, Integer clubCreadorId);

    @EntityGraph(attributePaths = {"hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor"})
    List<Producto> findByHubIdAndTipoAndEstadoAprobacion(Integer hubId, String tipo, String estadoAprobacion);

    @EntityGraph(attributePaths = {"hub", "clubCreador", "clubCreador.anfitrion", "revisadoPor"})
    List<Producto> findByClubCreadorIdAndTipoAndEstadoAprobacion(Integer clubCreadorId, String tipo, String estadoAprobacion);
}
