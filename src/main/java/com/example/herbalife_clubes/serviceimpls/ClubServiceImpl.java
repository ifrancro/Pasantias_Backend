package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.club.ClubDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.ClubMapper;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.ClubService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ClubServiceImpl implements ClubService {
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private HubRepository hubRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public ClubDTO createClub(ClubDTO clubDTO, Integer hubId, Integer anfitrionId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResourceNotFoundException("Hub no encontrado con id: " + hubId));
        Usuario anfitrion = usuarioRepository.findById(anfitrionId)
                .orElseThrow(() -> new ResourceNotFoundException("Anfitrión no encontrado con id: " + anfitrionId));
        
        Club club = ClubMapper.mapClubDTOToClub(clubDTO);
        club.setHub(hub);
        club.setAnfitrion(anfitrion);
        if (club.getEstado() == null) {
            club.setEstado("PENDIENTE");
        }
        
        Club savedClub = clubRepository.save(club);
        return ClubMapper.mapClubToClubDTO(savedClub);
    }

    @Override
    public ClubDTO updateClub(Integer clubId, ClubDTO clubDTO) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        
        club.setNombreClub(clubDTO.getNombreClub());
        club.setDireccion(clubDTO.getDireccion());
        club.setHorario(clubDTO.getHorario());
        club.setLat(clubDTO.getLat());
        club.setLng(clubDTO.getLng());
        
        Club updatedClub = clubRepository.save(club);
        return ClubMapper.mapClubToClubDTO(updatedClub);
    }

    @Override
    public ClubDTO getClub(Integer clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        return ClubMapper.mapClubToClubDTO(club);
    }

    @Override
    public List<ClubDTO> getAllClubes() {
        List<Club> clubes = clubRepository.findAll();
        return clubes.stream()
                .map(ClubMapper::mapClubToClubDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClubDTO> getClubesByHub(Integer hubId) {
        List<Club> clubes = clubRepository.findByHubId(hubId);
        return clubes.stream()
                .map(ClubMapper::mapClubToClubDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ClubDTO aprobarClub(Integer clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        club.setEstado("APROBADO");
        Club updatedClub = clubRepository.save(club);
        return ClubMapper.mapClubToClubDTO(updatedClub);
    }

    @Override
    public ClubDTO rechazarClub(Integer clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        club.setEstado("RECHAZADO");
        Club updatedClub = clubRepository.save(club);
        return ClubMapper.mapClubToClubDTO(updatedClub);
    }

    @Override
    public ClubDTO activarClub(Integer clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        club.setEstado("ACTIVO");
        Club updatedClub = clubRepository.save(club);
        return ClubMapper.mapClubToClubDTO(updatedClub);
    }

    @Override
    public ClubDTO desactivarClub(Integer clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        club.setEstado("INACTIVO");
        Club updatedClub = clubRepository.save(club);
        return ClubMapper.mapClubToClubDTO(updatedClub);
    }

    @Override
    public List<ClubDTO> getClubesActivos() {
        // Mostrar clubes con estado ACTIVO o APROBADO (visibles al público)
        List<Club> clubes = clubRepository.findByEstadoIn(List.of("ACTIVO", "APROBADO"));
        return clubes.stream()
                .map(ClubMapper::mapClubToClubDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ClubDTO getClubActivo(Integer clubId) {
        // Mostrar club si está ACTIVO o APROBADO (visible al público)
        Club club = clubRepository.findByIdAndEstadoIn(clubId, List.of("ACTIVO", "APROBADO"))
                .orElseThrow(() -> new ResourceNotFoundException("Club activo o aprobado no encontrado con id: " + clubId));
        return ClubMapper.mapClubToClubDTO(club);
    }
}

