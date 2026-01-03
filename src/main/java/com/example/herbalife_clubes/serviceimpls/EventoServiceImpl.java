package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.evento.EventoDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Evento;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.EventoMapper;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.EventoRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.services.EventoService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class EventoServiceImpl implements EventoService {
    @Autowired
    private EventoRepository eventoRepository;
    @Autowired
    private HubRepository hubRepository;
    @Autowired
    private ClubRepository clubRepository;

    @Override
    public EventoDTO createEvento(EventoDTO eventoDTO, Integer hubId, Integer clubId) {
        Evento evento = EventoMapper.mapEventoDTOToEvento(eventoDTO);
        
        if (hubId != null) {
            Hub hub = hubRepository.findById(hubId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hub no encontrado con id: " + hubId));
            evento.setHub(hub);
        }
        
        if (clubId != null) {
            Club club = clubRepository.findById(clubId)
                    .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
            evento.setClub(club);
        }
        
        Evento savedEvento = eventoRepository.save(evento);
        return EventoMapper.mapEventoToEventoDTO(savedEvento);
    }

    @Override
    public EventoDTO updateEvento(Integer eventoId, EventoDTO eventoDTO) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + eventoId));
        
        evento.setNombre(eventoDTO.getNombre());
        evento.setFechaEvento(eventoDTO.getFechaEvento());
        evento.setDescripcion(eventoDTO.getDescripcion());
        
        Evento updatedEvento = eventoRepository.save(evento);
        return EventoMapper.mapEventoToEventoDTO(updatedEvento);
    }

    @Override
    public String deleteEvento(Integer eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + eventoId));
        eventoRepository.delete(evento);
        return "Evento ha sido eliminado";
    }

    @Override
    public EventoDTO getEvento(Integer eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + eventoId));
        return EventoMapper.mapEventoToEventoDTO(evento);
    }

    @Override
    public List<EventoDTO> getEventos() {
        List<Evento> eventos = eventoRepository.findAll();
        return eventos.stream()
                .map(EventoMapper::mapEventoToEventoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventoDTO> getEventosByHub(Integer hubId) {
        List<Evento> eventos = eventoRepository.findByHubId(hubId);
        return eventos.stream()
                .map(EventoMapper::mapEventoToEventoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventoDTO> getEventosByClub(Integer clubId) {
        List<Evento> eventos = eventoRepository.findByClubId(clubId);
        return eventos.stream()
                .map(EventoMapper::mapEventoToEventoDTO)
                .collect(Collectors.toList());
    }
}

