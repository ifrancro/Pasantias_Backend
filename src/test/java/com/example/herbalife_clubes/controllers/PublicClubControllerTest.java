package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.club.ClubDTO;
import com.example.herbalife_clubes.exceptions.GlobalExceptionHandler;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.services.ClubService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicClubControllerTest {

    @Mock
    private ClubService clubService;

    @InjectMocks
    private PublicClubController publicClubController;

    @Test
    void listadoPublicoDevuelve200ConHubYAnfitrion() {
        when(clubService.getClubesActivos()).thenReturn(List.of(clubDto()));

        ResponseEntity<List<ClubDTO>> response = publicClubController.getClubesActivos();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        ClubDTO dto = response.getBody().get(0);
        assertEquals(10, dto.getHubId());
        assertEquals("HUB Santa Cruz", dto.getHubNombre());
        assertEquals(20, dto.getAnfitrionId());
        assertEquals("Andrea Anfitriona", dto.getAnfitrionNombre());
    }

    @Test
    void detallePublicoDevuelve200ConLasMismasRelaciones() {
        when(clubService.getClubActivo(1)).thenReturn(clubDto());

        ResponseEntity<ClubDTO> response = publicClubController.getClubActivo(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("HUB Santa Cruz", response.getBody().getHubNombre());
        assertEquals("Andrea Anfitriona", response.getBody().getAnfitrionNombre());
    }

    @Test
    void detallePublicoInexistenteSigueSiendo404() {
        when(clubService.getClubActivo(999))
                .thenThrow(new ResourceNotFoundException("Club activo o aprobado no encontrado con id: 999"));

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class, () -> publicClubController.getClubActivo(999));

        ResponseEntity<?> handled = new GlobalExceptionHandler().handleResourceNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, handled.getStatusCode());
    }

    private static ClubDTO clubDto() {
        ClubDTO dto = new ClubDTO();
        dto.setId(1);
        dto.setHubId(10);
        dto.setHubNombre("HUB Santa Cruz");
        dto.setAnfitrionId(20);
        dto.setAnfitrionNombre("Andrea Anfitriona");
        dto.setNombreClub("Club Demo");
        dto.setEstado("ACTIVO");
        return dto;
    }
}
