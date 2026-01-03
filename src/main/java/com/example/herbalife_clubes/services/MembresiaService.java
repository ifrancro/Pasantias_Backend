package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.membresia.MembresiaDTO;

import java.util.List;

public interface MembresiaService {
    MembresiaDTO createMembresia(MembresiaDTO membresiaDTO, Integer usuarioId, Integer clubId, Integer nivelId);
    MembresiaDTO getMembresia(Integer membresiaId);
    MembresiaDTO getMembresiaByUsuario(Integer usuarioId);
    List<MembresiaDTO> getMembresiasByClub(Integer clubId);
    MembresiaDTO cambiarEstado(Integer membresiaId, String estado);
    MembresiaDTO cambiarNivel(Integer membresiaId, Integer nivelId);
    MembresiaDTO actualizarPuntos(Integer membresiaId, Integer puntos);
}

