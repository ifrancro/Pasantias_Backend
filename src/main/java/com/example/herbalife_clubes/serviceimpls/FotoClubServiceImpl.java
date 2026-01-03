package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.fotoclub.FotoClubDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.FotoClub;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.FotoClubMapper;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.FotoClubRepository;
import com.example.herbalife_clubes.services.FotoClubService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class FotoClubServiceImpl implements FotoClubService {
    @Autowired
    private FotoClubRepository fotoClubRepository;
    @Autowired
    private ClubRepository clubRepository;

    @Override
    public FotoClubDTO subirFoto(Integer clubId, String urlFoto, String tipo) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        
        FotoClub foto = new FotoClub();
        foto.setClub(club);
        foto.setUrlFoto(urlFoto);
        foto.setTipo(tipo != null ? tipo : "GENERAL");
        
        FotoClub savedFoto = fotoClubRepository.save(foto);
        return FotoClubMapper.mapFotoClubToFotoClubDTO(savedFoto);
    }

    @Override
    public String eliminarFoto(Integer fotoId) {
        FotoClub foto = fotoClubRepository.findById(fotoId)
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada con id: " + fotoId));
        fotoClubRepository.delete(foto);
        return "Foto eliminada exitosamente";
    }

    @Override
    public List<FotoClubDTO> listarFotosByClub(Integer clubId) {
        List<FotoClub> fotos = fotoClubRepository.findByClubId(clubId);
        return fotos.stream()
                .map(FotoClubMapper::mapFotoClubToFotoClubDTO)
                .collect(Collectors.toList());
    }
}

