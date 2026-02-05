package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.club.ClubDTO;

import java.util.List;

public interface ClubService {
    ClubDTO createClub(ClubDTO clubDTO, Integer hubId, Integer anfitrionId);
    ClubDTO updateClub(Integer clubId, ClubDTO clubDTO);
    ClubDTO getClub(Integer clubId);
    List<ClubDTO> getAllClubes();
    List<ClubDTO> getClubesByHub(Integer hubId);
    ClubDTO aprobarClub(Integer clubId);
    ClubDTO rechazarClub(Integer clubId);
    ClubDTO activarClub(Integer clubId);
    ClubDTO desactivarClub(Integer clubId);
    
    // Métodos públicos para obtener solo clubes activos
    List<ClubDTO> getClubesActivos();
    ClubDTO getClubActivo(Integer clubId);
    
    /**
     * Obtiene el club del anfitrión autenticado.
     * Busca el club donde clubes.anfitrion_id == usuario_autenticado.id
     * @param usuarioId ID del usuario autenticado
     * @return ClubDTO del club del anfitrión
     */
    ClubDTO getClubByAnfitrion(Integer usuarioId);
}

