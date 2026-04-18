package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.logro.LogroDTO;
import com.example.herbalife_clubes.dtos.logro.LogroProgresoDTO;

import java.util.List;

public interface LogroService {
    /**
     * Crea un logro según el rol del usuario:
     * - ADMIN: Logro global (club_creador_id = null, estado = APROBADO)
     * - ANFITRION: Logro local (club_creador_id = club del anfitrión, estado = PENDIENTE)
     * 
     * @param logroDTO DTO con los datos del logro
     * @param usuarioId ID del usuario autenticado
     * @return LogroDTO creado
     */
    LogroDTO createLogro(LogroDTO logroDTO, Integer usuarioId);
    LogroDTO updateLogro(Integer logroId, LogroDTO logroDTO);
    String deleteLogro(Integer logroId);
    LogroDTO getLogro(Integer logroId);
    List<LogroDTO> getLogros();
    LogroDTO activarLogro(Integer logroId);
    LogroDTO inactivarLogro(Integer logroId);
    
    /**
     * Cambia el estado de aprobación de un logro (solo ADMIN).
     * 
     * @param logroId ID del logro
     * @param estadoAprobacion Nuevo estado (APROBADO | RECHAZADO)
     * @return LogroDTO actualizado
     */
    LogroDTO cambiarEstadoAprobacion(Integer logroId, String estadoAprobacion);

    /**
     * Progreso del socio (por membresía) en retos compuestos vigentes y aprobados.
     * @param membresiaId id de la tabla membresias (socio en su club)
     */
    List<LogroProgresoDTO> getProgresoSocio(Integer membresiaId);
}

