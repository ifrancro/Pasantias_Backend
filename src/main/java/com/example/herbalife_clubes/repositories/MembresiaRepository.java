package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Membresia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembresiaRepository extends JpaRepository<Membresia, Integer> {
    Optional<Membresia> findByUsuarioId(Integer usuarioId);
    List<Membresia> findByClubId(Integer clubId);
}

