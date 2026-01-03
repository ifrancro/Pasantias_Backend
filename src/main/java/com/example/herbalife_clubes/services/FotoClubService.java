package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.fotoclub.FotoClubDTO;

import java.util.List;

public interface FotoClubService {
    FotoClubDTO subirFoto(Integer clubId, String urlFoto, String tipo);
    String eliminarFoto(Integer fotoId);
    List<FotoClubDTO> listarFotosByClub(Integer clubId);
}

