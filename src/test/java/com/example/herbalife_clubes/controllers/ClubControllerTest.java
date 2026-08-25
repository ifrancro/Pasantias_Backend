package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.club.ClubDTO;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.GlobalExceptionHandler;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.ClubService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubControllerTest {

    @Mock
    private ClubService clubService;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ClubController clubController;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllClubesDevuelve200ConHubYAnfitrion() {
        when(clubService.getAllClubes()).thenReturn(List.of(clubDto()));

        ResponseEntity<List<ClubDTO>> response = clubController.getAllClubes(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertClubJsonContract(response.getBody().get(0));
    }

    @Test
    void getMiClubDevuelve200ConLasMismasRelaciones() {
        authenticateAs("andrea@demo.com");
        Usuario anfitrion = new Usuario();
        anfitrion.setId(20);
        anfitrion.setEmail("andrea@demo.com");
        when(usuarioRepository.findByEmail("andrea@demo.com")).thenReturn(Optional.of(anfitrion));
        when(clubService.getClubByAnfitrion(20)).thenReturn(clubDto());

        ResponseEntity<ClubDTO> response = clubController.getMiClub();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertClubJsonContract(response.getBody());
    }

    @Test
    void getClubInexistentePropagaNotFoundQueElHandlerConvierteEn404() {
        when(clubService.getClub(999))
                .thenThrow(new ResourceNotFoundException("Club no encontrado con id: 999"));

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class, () -> clubController.getClub(999));

        ResponseEntity<?> handled = new GlobalExceptionHandler().handleResourceNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, handled.getStatusCode());
        assertNotNull(handled.getBody());
        assertTrue(handled.getBody().toString().contains("999"));
    }

    @Test
    void getMiClubSinClubPropagaNotFound() {
        authenticateAs("admin@demo.com");
        Usuario admin = new Usuario();
        admin.setId(1);
        admin.setEmail("admin@demo.com");
        when(usuarioRepository.findByEmail("admin@demo.com")).thenReturn(Optional.of(admin));
        when(clubService.getClubByAnfitrion(1))
                .thenThrow(new ResourceNotFoundException("No se encontró ningún club para el anfitrión con id: 1"));

        assertThrows(ResourceNotFoundException.class, () -> clubController.getMiClub());
    }

    private static void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
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

    private static void assertClubJsonContract(ClubDTO dto) {
        assertNotNull(dto);
        assertEquals(10, dto.getHubId());
        assertEquals("HUB Santa Cruz", dto.getHubNombre());
        assertEquals(20, dto.getAnfitrionId());
        assertEquals("Andrea Anfitriona", dto.getAnfitrionNombre());
    }
}
