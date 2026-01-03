package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.notificacion.NotificacionDTO;
import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.NotificacionMapper;
import com.example.herbalife_clubes.repositories.*;
import com.example.herbalife_clubes.services.NotificacionService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {
    @Autowired
    private NotificacionRepository notificacionRepository;
    @Autowired
    private HubRepository hubRepository;
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PedidoRepository pedidoRepository;

    @Override
    public NotificacionDTO enviarNotificacion(NotificacionDTO notificacionDTO, Integer hubId, Integer clubId, 
                                               Integer usuarioId, Integer pedidoId) {
        Notificacion notificacion = new Notificacion();
        notificacion.setTitulo(notificacionDTO.getTitulo());
        notificacion.setMensaje(notificacionDTO.getMensaje());
        notificacion.setTipoSegmentacion(notificacionDTO.getTipoSegmentacion());
        
        if (hubId != null) {
            Hub hub = hubRepository.findById(hubId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hub no encontrado con id: " + hubId));
            notificacion.setHub(hub);
        }
        
        if (clubId != null) {
            Club club = clubRepository.findById(clubId)
                    .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
            notificacion.setClub(club);
        }
        
        if (usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
            notificacion.setUsuario(usuario);
        }
        
        if (pedidoId != null) {
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId));
            notificacion.setPedido(pedido);
        }
        
        Notificacion savedNotificacion = notificacionRepository.save(notificacion);
        return NotificacionMapper.mapNotificacionToNotificacionDTO(savedNotificacion);
    }

    @Override
    public List<NotificacionDTO> getHistorialByUsuario(Integer usuarioId) {
        List<Notificacion> notificaciones = notificacionRepository.findByUsuarioId(usuarioId);
        return notificaciones.stream()
                .map(NotificacionMapper::mapNotificacionToNotificacionDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificacionDTO> getHistorialByHub(Integer hubId) {
        List<Notificacion> notificaciones = notificacionRepository.findByHubId(hubId);
        return notificaciones.stream()
                .map(NotificacionMapper::mapNotificacionToNotificacionDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificacionDTO> getHistorialByClub(Integer clubId) {
        List<Notificacion> notificaciones = notificacionRepository.findByClubId(clubId);
        return notificaciones.stream()
                .map(NotificacionMapper::mapNotificacionToNotificacionDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificacionDTO> getHistorial() {
        List<Notificacion> notificaciones = notificacionRepository.findAll();
        return notificaciones.stream()
                .map(NotificacionMapper::mapNotificacionToNotificacionDTO)
                .collect(Collectors.toList());
    }
}

