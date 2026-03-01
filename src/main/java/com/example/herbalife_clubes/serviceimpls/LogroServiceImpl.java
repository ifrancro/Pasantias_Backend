package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.logro.LogroDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Logro;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.LogroMapper;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.LogroRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.LogroService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class LogroServiceImpl implements LogroService {
    @Autowired
    private LogroRepository logroRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ClubRepository clubRepository;

    @Override
    public LogroDTO createLogro(LogroDTO logroDTO, Integer usuarioId) {
        // Obtener usuario para determinar su rol
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
        
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        
        // Validar fechas
        if (logroDTO.getFechaInicio() == null || logroDTO.getFechaFin() == null) {
            throw new IllegalArgumentException("fecha_inicio y fecha_fin son obligatorios");
        }
        
        if (logroDTO.getFechaInicio().isAfter(logroDTO.getFechaFin())) {
            throw new IllegalArgumentException("fecha_inicio no puede ser posterior a fecha_fin");
        }
        
        // Validar que fecha_inicio no sea anterior a hoy (opcional, según reglas de negocio)
        if (logroDTO.getFechaInicio().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("fecha_inicio no puede ser anterior a la fecha actual");
        }
        
        // Crear logro desde el DTO
        Logro logro = LogroMapper.mapLogroDTOToLogro(logroDTO);
        
        // Lógica según el rol
        if ("ADMIN".equalsIgnoreCase(rolNombre)) {
            // ADMIN: Logro global (club_creador_id = null, estado = APROBADO)
            logro.setClubCreador(null);
            logro.setEstadoAprobacion("APROBADO");
        } else if ("ANFITRION".equalsIgnoreCase(rolNombre)) {
            // ANFITRION: Logro local (club_creador_id = club del anfitrión, estado = PENDIENTE)
            // Obtener el club del anfitrión
            List<Club> clubes = clubRepository.findByAnfitrionId(usuarioId);
            if (clubes.isEmpty()) {
                throw new IllegalArgumentException("El anfitrión no tiene un club asociado");
            }
            Club clubAnfitrion = clubes.get(0); // Tomar el primero
            logro.setClubCreador(clubAnfitrion);
            logro.setEstadoAprobacion("PENDIENTE");
        } else {
            throw new IllegalArgumentException("Solo usuarios ADMIN o ANFITRION pueden crear logros");
        }
        
        // Guardar logro
        Logro savedLogro = logroRepository.save(logro);
        return LogroMapper.mapLogroToLogroDTO(savedLogro);
    }

    @Override
    public LogroDTO updateLogro(Integer logroId, LogroDTO logroDTO) {
        Logro logro = logroRepository.findById(logroId)
                .orElseThrow(() -> new ResourceNotFoundException("Logro no encontrado con id: " + logroId));
        
        // Validar fechas si se proporcionan
        if (logroDTO.getFechaInicio() != null && logroDTO.getFechaFin() != null) {
            if (logroDTO.getFechaInicio().isAfter(logroDTO.getFechaFin())) {
                throw new IllegalArgumentException("fecha_inicio no puede ser posterior a fecha_fin");
            }
        }
        
        logro.setNombre(logroDTO.getNombre());
        logro.setDescripcion(logroDTO.getDescripcion());
        logro.setIconoUrl(logroDTO.getIconoUrl());
        logro.setTipoRequisito(logroDTO.getTipoRequisito());
        logro.setTipoMetrica(logroDTO.getTipoMetrica());
        logro.setMetaCantidad(logroDTO.getMetaCantidad());
        logro.setPuntosRecompensa(logroDTO.getPuntosRecompensa());
        
        // Actualizar fechas si se proporcionan
        if (logroDTO.getFechaInicio() != null) {
            logro.setFechaInicio(logroDTO.getFechaInicio());
        }
        if (logroDTO.getFechaFin() != null) {
            logro.setFechaFin(logroDTO.getFechaFin());
        }
        
        // estadoAprobacion solo puede cambiarse mediante el endpoint específico de aprobación
        
        Logro updatedLogro = logroRepository.save(logro);
        return LogroMapper.mapLogroToLogroDTO(updatedLogro);
    }

    @Override
    public String deleteLogro(Integer logroId) {
        Logro logro = logroRepository.findById(logroId)
                .orElseThrow(() -> new ResourceNotFoundException("Logro no encontrado con id: " + logroId));
        logroRepository.delete(logro);
        return "Logro ha sido eliminado";
    }

    @Override
    public LogroDTO getLogro(Integer logroId) {
        Logro logro = logroRepository.findById(logroId)
                .orElseThrow(() -> new ResourceNotFoundException("Logro no encontrado con id: " + logroId));
        return LogroMapper.mapLogroToLogroDTO(logro);
    }

    @Override
    public List<LogroDTO> getLogros() {
        List<Logro> logros = logroRepository.findAll();
        return logros.stream()
                .map(LogroMapper::mapLogroToLogroDTO)
                .collect(Collectors.toList());
    }

    @Override
    public LogroDTO activarLogro(Integer logroId) {
        // Los logros no tienen campo activo, pero podemos usar un campo de estado si lo agregamos
        // Por ahora solo retornamos el logro
        return getLogro(logroId);
    }

    @Override
    public LogroDTO inactivarLogro(Integer logroId) {
        // Similar a activarLogro
        return getLogro(logroId);
    }

    @Override
    public LogroDTO cambiarEstadoAprobacion(Integer logroId, String estadoAprobacion) {
        // Validar que el estado sea válido
        if (!"APROBADO".equalsIgnoreCase(estadoAprobacion) && !"RECHAZADO".equalsIgnoreCase(estadoAprobacion)) {
            throw new IllegalArgumentException("El estado de aprobación debe ser APROBADO o RECHAZADO");
        }
        
        Logro logro = logroRepository.findById(logroId)
                .orElseThrow(() -> new ResourceNotFoundException("Logro no encontrado con id: " + logroId));
        
        logro.setEstadoAprobacion(estadoAprobacion.toUpperCase());
        Logro updatedLogro = logroRepository.save(logro);
        return LogroMapper.mapLogroToLogroDTO(updatedLogro);
    }
}

