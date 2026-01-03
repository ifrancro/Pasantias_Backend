package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.notificacion.NotificacionDTO;
import com.example.herbalife_clubes.entities.Notificacion;

public class NotificacionMapper {
    public static NotificacionDTO mapNotificacionToNotificacionDTO(Notificacion notificacion) {
        NotificacionDTO dto = new NotificacionDTO();
        dto.setId(notificacion.getId());
        dto.setTitulo(notificacion.getTitulo());
        dto.setMensaje(notificacion.getMensaje());
        dto.setTipoSegmentacion(notificacion.getTipoSegmentacion());
        dto.setHubId(notificacion.getHub() != null ? notificacion.getHub().getId() : null);
        dto.setHubNombre(notificacion.getHub() != null ? notificacion.getHub().getNombre() : null);
        dto.setClubId(notificacion.getClub() != null ? notificacion.getClub().getId() : null);
        dto.setClubNombre(notificacion.getClub() != null ? notificacion.getClub().getNombreClub() : null);
        dto.setUsuarioId(notificacion.getUsuario() != null ? notificacion.getUsuario().getId() : null);
        dto.setUsuarioNombre(notificacion.getUsuario() != null ? 
                notificacion.getUsuario().getNombre() + " " + notificacion.getUsuario().getApellido() : null);
        dto.setPedidoId(notificacion.getPedido() != null ? notificacion.getPedido().getId() : null);
        dto.setFechaEnvio(notificacion.getFechaEnvio());
        return dto;
    }
}

