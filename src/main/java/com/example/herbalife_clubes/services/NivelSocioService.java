package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.nivelsocio.NivelSocioDTO;

import java.util.List;

public interface NivelSocioService {
    NivelSocioDTO createNivelSocio(NivelSocioDTO nivelSocioDTO);
    NivelSocioDTO updateNivelSocio(Integer nivelId, NivelSocioDTO nivelSocioDTO);
    String deleteNivelSocio(Integer nivelId);
    NivelSocioDTO getNivelSocio(Integer nivelId);
    List<NivelSocioDTO> getNivelesSocio();
}

