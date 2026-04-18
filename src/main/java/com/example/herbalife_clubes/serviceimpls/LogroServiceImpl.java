package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.logro.LogroDTO;
import com.example.herbalife_clubes.dtos.logro.LogroProgresoDTO;
import com.example.herbalife_clubes.dtos.logro.RequisitoLogroDTO;
import com.example.herbalife_clubes.dtos.logro.RequisitoProgresoDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Logro;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.RequisitoLogro;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.LogroMapper;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.LogroRepository;
import com.example.herbalife_clubes.repositories.MembresiaLogroRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.LogroService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
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
    @Autowired
    private MembresiaRepository membresiaRepository;
    @Autowired
    private MembresiaLogroRepository membresiaLogroRepository;
    @Autowired
    private LogroMetricaCalculator logroMetricaCalculator;

    private void validarRequisitos(LogroDTO logroDTO) {
        if (logroDTO.getRequisitos() == null || logroDTO.getRequisitos().isEmpty()) {
            throw new IllegalArgumentException("Debe incluir al menos un requisito (tipoMetrica y cantidadEsperada)");
        }
        for (RequisitoLogroDTO r : logroDTO.getRequisitos()) {
            if (r.getTipoMetrica() == null || r.getTipoMetrica().isBlank()) {
                throw new IllegalArgumentException("Cada requisito debe tener tipoMetrica (ej. ASISTENCIA, CONSUMO, REFERIDOS, RACHA)");
            }
            if (r.getCantidadEsperada() == null || r.getCantidadEsperada() <= 0) {
                throw new IllegalArgumentException("cantidadEsperada debe ser mayor a 0 en cada requisito");
            }
        }
    }

    @Override
    @Transactional
    public LogroDTO createLogro(LogroDTO logroDTO, Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));

        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";

        if (logroDTO.getFechaInicio() == null || logroDTO.getFechaFin() == null) {
            throw new IllegalArgumentException("fecha_inicio y fecha_fin son obligatorios");
        }

        if (logroDTO.getFechaInicio().isAfter(logroDTO.getFechaFin())) {
            throw new IllegalArgumentException("fecha_inicio no puede ser posterior a fecha_fin");
        }

        if (logroDTO.getFechaInicio().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("fecha_inicio no puede ser anterior a la fecha actual");
        }

        validarRequisitos(logroDTO);

        Logro logro = LogroMapper.mapLogroDTOToLogro(logroDTO);

        if ("ADMIN".equalsIgnoreCase(rolNombre)) {
            logro.setClubCreador(null);
            logro.setEstadoAprobacion("APROBADO");
        } else if ("ANFITRION".equalsIgnoreCase(rolNombre)) {
            List<Club> clubes = clubRepository.findByAnfitrionId(usuarioId);
            if (clubes.isEmpty()) {
                throw new IllegalArgumentException("El anfitrión no tiene un club asociado");
            }
            Club clubAnfitrion = clubes.get(0);
            logro.setClubCreador(clubAnfitrion);
            logro.setEstadoAprobacion("PENDIENTE");
        } else {
            throw new IllegalArgumentException("Solo usuarios ADMIN o ANFITRION pueden crear logros");
        }

        Logro savedLogro = logroRepository.save(logro);
        return LogroMapper.mapLogroToLogroDTO(savedLogro);
    }

    @Override
    @Transactional
    public LogroDTO updateLogro(Integer logroId, LogroDTO logroDTO) {
        Logro logro = logroRepository.findByIdWithRequisitos(logroId)
                .orElseThrow(() -> new ResourceNotFoundException("Logro no encontrado con id: " + logroId));

        if (logroDTO.getFechaInicio() != null && logroDTO.getFechaFin() != null) {
            if (logroDTO.getFechaInicio().isAfter(logroDTO.getFechaFin())) {
                throw new IllegalArgumentException("fecha_inicio no puede ser posterior a fecha_fin");
            }
        }

        if (logroDTO.getRequisitos() != null) {
            validarRequisitos(logroDTO);
        }

        logro.setNombre(logroDTO.getNombre());
        logro.setDescripcion(logroDTO.getDescripcion());
        logro.setIconoUrl(logroDTO.getIconoUrl());
        logro.setPuntosRecompensa(logroDTO.getPuntosRecompensa());

        if (logroDTO.getFechaInicio() != null) {
            logro.setFechaInicio(logroDTO.getFechaInicio());
        }
        if (logroDTO.getFechaFin() != null) {
            logro.setFechaFin(logroDTO.getFechaFin());
        }

        if (logroDTO.getRequisitos() != null) {
            logro.getRequisitos().clear();
            for (RequisitoLogroDTO rd : logroDTO.getRequisitos()) {
                RequisitoLogro r = new RequisitoLogro();
                r.setTipoMetrica(rd.getTipoMetrica() != null ? rd.getTipoMetrica().trim().toUpperCase() : null);
                r.setCantidadEsperada(rd.getCantidadEsperada());
                r.setLogro(logro);
                logro.getRequisitos().add(r);
            }
        }

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
        Logro logro = logroRepository.findByIdWithRequisitos(logroId)
                .orElseThrow(() -> new ResourceNotFoundException("Logro no encontrado con id: " + logroId));
        return LogroMapper.mapLogroToLogroDTO(logro);
    }

    @Override
    public List<LogroDTO> getLogros() {
        return logroRepository.findAllWithRequisitos().stream()
                .map(LogroMapper::mapLogroToLogroDTO)
                .collect(Collectors.toList());
    }

    @Override
    public LogroDTO activarLogro(Integer logroId) {
        return getLogro(logroId);
    }

    @Override
    public LogroDTO inactivarLogro(Integer logroId) {
        return getLogro(logroId);
    }

    @Override
    public LogroDTO cambiarEstadoAprobacion(Integer logroId, String estadoAprobacion) {
        if (!"APROBADO".equalsIgnoreCase(estadoAprobacion) && !"RECHAZADO".equalsIgnoreCase(estadoAprobacion)) {
            throw new IllegalArgumentException("El estado de aprobación debe ser APROBADO o RECHAZADO");
        }

        Logro logro = logroRepository.findByIdWithRequisitos(logroId)
                .orElseThrow(() -> new ResourceNotFoundException("Logro no encontrado con id: " + logroId));

        logro.setEstadoAprobacion(estadoAprobacion.toUpperCase());
        Logro updatedLogro = logroRepository.save(logro);
        return LogroMapper.mapLogroToLogroDTO(updatedLogro);
    }

    @Override
    public List<LogroProgresoDTO> getProgresoSocio(Integer membresiaId) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));

        List<LogroProgresoDTO> resultado = new ArrayList<>();
        for (Logro logro : logroRepository.findAllWithRequisitos()) {
            if (!logroMetricaCalculator.aplicaAMembresia(logro, membresia)) {
                continue;
            }
            if (logro.getRequisitos() == null || logro.getRequisitos().isEmpty()) {
                continue;
            }

            LogroProgresoDTO dto = new LogroProgresoDTO();
            dto.setLogroId(logro.getId());
            dto.setNombre(logro.getNombre());
            dto.setPuntosRecompensa(logro.getPuntosRecompensa());

            boolean yaObtenido = membresiaLogroRepository.findByMembresiaIdAndLogroId(membresiaId, logro.getId()).isPresent();
            boolean cumple = logroMetricaCalculator.cumpleTodosRequisitos(membresia, logro);
            dto.setCompletado(yaObtenido || cumple);

            List<RequisitoProgresoDTO> reqs = new ArrayList<>();
            for (RequisitoLogro req : logro.getRequisitos()) {
                int actual = logroMetricaCalculator.cantidadActual(membresia, logro, req);
                reqs.add(new RequisitoProgresoDTO(req.getTipoMetrica(), req.getCantidadEsperada(), actual));
            }
            dto.setRequisitos(reqs);
            resultado.add(dto);
        }
        return resultado;
    }
}
