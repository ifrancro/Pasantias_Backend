package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.logro.LogroDTO;

import java.util.List;

public interface LogroService {
    LogroDTO createLogro(LogroDTO logroDTO);
    LogroDTO updateLogro(Integer logroId, LogroDTO logroDTO);
    String deleteLogro(Integer logroId);
    LogroDTO getLogro(Integer logroId);
    List<LogroDTO> getLogros();
    LogroDTO activarLogro(Integer logroId);
    LogroDTO inactivarLogro(Integer logroId);
}

