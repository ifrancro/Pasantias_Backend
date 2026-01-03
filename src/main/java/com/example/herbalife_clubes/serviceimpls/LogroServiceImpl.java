package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.logro.LogroDTO;
import com.example.herbalife_clubes.entities.Logro;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.LogroMapper;
import com.example.herbalife_clubes.repositories.LogroRepository;
import com.example.herbalife_clubes.services.LogroService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class LogroServiceImpl implements LogroService {
    @Autowired
    private LogroRepository logroRepository;

    @Override
    public LogroDTO createLogro(LogroDTO logroDTO) {
        Logro logro = LogroMapper.mapLogroDTOToLogro(logroDTO);
        Logro savedLogro = logroRepository.save(logro);
        return LogroMapper.mapLogroToLogroDTO(savedLogro);
    }

    @Override
    public LogroDTO updateLogro(Integer logroId, LogroDTO logroDTO) {
        Logro logro = logroRepository.findById(logroId)
                .orElseThrow(() -> new ResourceNotFoundException("Logro no encontrado con id: " + logroId));
        
        logro.setNombre(logroDTO.getNombre());
        logro.setDescripcion(logroDTO.getDescripcion());
        logro.setIconoUrl(logroDTO.getIconoUrl());
        logro.setTipoRequisito(logroDTO.getTipoRequisito());
        
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
}

