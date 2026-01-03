package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.SoporteTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoporteTicketRepository extends JpaRepository<SoporteTicket, Integer> {
    List<SoporteTicket> findByUsuarioId(Integer usuarioId);
}

