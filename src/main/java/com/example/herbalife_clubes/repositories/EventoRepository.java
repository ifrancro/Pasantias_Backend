package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Integer> {
    List<Evento> findByHubId(Integer hubId);
    List<Evento> findByClubId(Integer clubId);
}

