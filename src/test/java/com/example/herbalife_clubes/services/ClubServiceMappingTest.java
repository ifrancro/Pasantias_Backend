package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.club.ClubDTO;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.ClubMapperTest;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.serviceimpls.ClubServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubServiceMappingTest {

    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private ClubServiceImpl clubService;

    @Test
    void getAllClubesMapeaHubYAnfitrionSinLazyInitialization() {
        when(clubRepository.findAll()).thenReturn(List.of(ClubMapperTest.clubConRelaciones()));

        List<ClubDTO> result = assertDoesNotThrow(clubService::getAllClubes);

        assertEquals(1, result.size());
        assertClubDto(result.get(0));
        verify(clubRepository).findAll();
    }

    @Test
    void getClubByAnfitrionMapeaLasMismasRelaciones() {
        when(clubRepository.findByAnfitrionId(20)).thenReturn(List.of(ClubMapperTest.clubConRelaciones()));

        ClubDTO dto = assertDoesNotThrow(() -> clubService.getClubByAnfitrion(20));

        assertClubDto(dto);
        verify(clubRepository).findByAnfitrionId(20);
    }

    @Test
    void getClubesActivosUsaConsultaConRelacionesYMapeaDto() {
        when(clubRepository.findByEstadoIn(List.of("ACTIVO", "APROBADO")))
                .thenReturn(List.of(ClubMapperTest.clubConRelaciones()));

        List<ClubDTO> result = clubService.getClubesActivos();

        assertEquals(1, result.size());
        assertClubDto(result.get(0));
        verify(clubRepository).findByEstadoIn(List.of("ACTIVO", "APROBADO"));
    }

    @Test
    void getClubActivoMapeaClubPublico() {
        when(clubRepository.findByIdAndEstadoIn(1, List.of("ACTIVO", "APROBADO")))
                .thenReturn(Optional.of(ClubMapperTest.clubConRelaciones()));

        ClubDTO dto = clubService.getClubActivo(1);

        assertClubDto(dto);
        verify(clubRepository).findByIdAndEstadoIn(1, List.of("ACTIVO", "APROBADO"));
    }

    @Test
    void getClubesByHubMapeaHubYAnfitrion() {
        when(clubRepository.findByHubId(10)).thenReturn(List.of(ClubMapperTest.clubConRelaciones()));

        List<ClubDTO> result = clubService.getClubesByHub(10);

        assertEquals(1, result.size());
        assertClubDto(result.get(0));
        verify(clubRepository).findByHubId(10);
    }

    @Test
    void getClubInexistenteSigueSiendoNotFound() {
        when(clubRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class, () -> clubService.getClub(999));
        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    void getClubActivoInexistenteSigueSiendoNotFound() {
        when(clubRepository.findByIdAndEstadoIn(999, List.of("ACTIVO", "APROBADO")))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class, () -> clubService.getClubActivo(999));
        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    void getClubByAnfitrionSinClubSigueSiendoNotFound() {
        when(clubRepository.findByAnfitrionId(77)).thenReturn(List.of());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class, () -> clubService.getClubByAnfitrion(77));
        assertTrue(ex.getMessage().contains("77"));
    }

    private static void assertClubDto(ClubDTO dto) {
        assertEquals(1, dto.getId());
        assertEquals(10, dto.getHubId());
        assertEquals("HUB Santa Cruz", dto.getHubNombre());
        assertEquals(20, dto.getAnfitrionId());
        assertEquals("Andrea Anfitriona", dto.getAnfitrionNombre());
    }
}
