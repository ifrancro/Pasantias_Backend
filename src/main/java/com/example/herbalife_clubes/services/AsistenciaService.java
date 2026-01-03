package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.asistencia.AsistenciaDTO;

import java.util.List;

public interface AsistenciaService {
    AsistenciaDTO registrarAsistencia(Integer membresiaId, Integer clubId, String qrClub);
    List<AsistenciaDTO> listarAsistenciasBySocio(Integer membresiaId);
    List<AsistenciaDTO> listarAsistenciasByClub(Integer clubId);
    List<AsistenciaDTO> listarTodasAsistencias();
}

