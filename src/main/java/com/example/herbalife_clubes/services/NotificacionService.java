package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.notificacion.NotificacionDTO;

import java.util.List;

public interface NotificacionService {
    NotificacionDTO enviarNotificacion(NotificacionDTO notificacionDTO, Integer hubId, Integer clubId, Integer usuarioId, Integer pedidoId);
    List<NotificacionDTO> getHistorialByUsuario(Integer usuarioId);
    List<NotificacionDTO> getHistorialByHub(Integer hubId);
    List<NotificacionDTO> getHistorialByClub(Integer clubId);
    List<NotificacionDTO> getHistorial();
}

