package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.evento.EventoDTO;

import java.util.List;

public interface EventoService {
    EventoDTO createEvento(EventoDTO eventoDTO, Integer hubId, Integer clubId);
    EventoDTO updateEvento(Integer eventoId, EventoDTO eventoDTO);
    String deleteEvento(Integer eventoId);
    EventoDTO getEvento(Integer eventoId);
    List<EventoDTO> getEventos();
    List<EventoDTO> getEventosByHub(Integer hubId);
    List<EventoDTO> getEventosByClub(Integer clubId);
}

