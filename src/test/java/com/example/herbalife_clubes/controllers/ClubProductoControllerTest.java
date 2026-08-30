package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.producto.ProductoConDisponibilidadDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.dtos.producto.PrecioVentaClubRequestDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.ProductoService;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubProductoControllerTest {

    @Mock
    private ProductoService productoService;
    @Mock
    private ClubRepository clubRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ClubProductoController controller;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void toggleSinJwtDevuelve401() {
        ResponseEntity<?> response = controller.toggleDisponibilidadProducto(3, 10);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(productoService, never()).toggleDisponibilidadEnClub(anyInt(), anyInt());
    }

    @Test
    void toggleEnOtroClubDevuelve403() {
        authenticateAs("host@club.com");
        Usuario host = usuario(20, "ANFITRION");
        when(usuarioRepository.findByEmail("host@club.com")).thenReturn(Optional.of(host));
        when(clubRepository.findByIdAndAnfitrionId(9, 20)).thenReturn(Optional.empty());
        Club ajeno = new Club();
        ajeno.setId(9);
        ajeno.setAnfitrion(usuario(99, "ANFITRION"));
        when(clubRepository.findById(9)).thenReturn(Optional.of(ajeno));

        ResponseEntity<?> response = controller.toggleDisponibilidadProducto(9, 10);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(productoService, never()).toggleDisponibilidadEnClub(anyInt(), anyInt());
    }

    @Test
    void socioNoPuedeHacerToggle() {
        authenticateAs("socio@club.com");
        Usuario socio = usuario(40, "SOCIO");
        when(usuarioRepository.findByEmail("socio@club.com")).thenReturn(Optional.of(socio));
        when(clubRepository.findByIdAndAnfitrionId(3, 40)).thenReturn(Optional.empty());
        when(clubRepository.findById(3)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.toggleDisponibilidadProducto(3, 10);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(productoService, never()).toggleDisponibilidadEnClub(anyInt(), anyInt());
    }

    @Test
    void anfitrionDuenoLlamaAlService() {
        authenticateAs("host@club.com");
        Usuario host = usuario(20, "ANFITRION");
        when(usuarioRepository.findByEmail("host@club.com")).thenReturn(Optional.of(host));
        when(clubRepository.findByIdAndAnfitrionId(3, 20)).thenReturn(Optional.of(new Club()));
        ProductoConDisponibilidadDTO dto = ProductoConDisponibilidadDTO.builder()
                .id(10)
                .disponible(false)
                .activo(true)
                .build();
        when(productoService.toggleDisponibilidadEnClub(3, 10)).thenReturn(dto);

        ResponseEntity<ProductoConDisponibilidadDTO> response =
                controller.toggleDisponibilidadProducto(3, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(false, response.getBody().getDisponible());
        verify(productoService).toggleDisponibilidadEnClub(3, 10);
    }

    @Test
    void patchPrecioSinJwtDevuelve401() {
        ResponseEntity<?> response = controller.actualizarPrecioVenta(3, 10, new PrecioVentaClubRequestDTO());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(productoService, never()).actualizarPrecioVentaEnClub(anyInt(), anyInt(), any());
    }

    @Test
    void patchPrecioEnOtroClubDevuelve403() {
        authenticateAs("host@club.com");
        Usuario host = usuario(20, "ANFITRION");
        when(usuarioRepository.findByEmail("host@club.com")).thenReturn(Optional.of(host));
        when(clubRepository.findByIdAndAnfitrionId(9, 20)).thenReturn(Optional.empty());
        Club ajeno = new Club();
        ajeno.setId(9);
        ajeno.setAnfitrion(usuario(99, "ANFITRION"));
        when(clubRepository.findById(9)).thenReturn(Optional.of(ajeno));

        ResponseEntity<?> response = controller.actualizarPrecioVenta(
                9, 10, new PrecioVentaClubRequestDTO(new java.math.BigDecimal("28.50")));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(productoService, never()).actualizarPrecioVentaEnClub(anyInt(), anyInt(), any());
    }

    @Test
    void anfitrionDuenoCambiaPrecioLlamaAlService() {
        authenticateAs("host@club.com");
        Usuario host = usuario(20, "ANFITRION");
        when(usuarioRepository.findByEmail("host@club.com")).thenReturn(Optional.of(host));
        when(clubRepository.findByIdAndAnfitrionId(3, 20)).thenReturn(Optional.of(new Club()));
        ProductoDTO dto = new ProductoDTO();
        dto.setId(10);
        when(productoService.actualizarPrecioVentaEnClub(3, 10, new java.math.BigDecimal("28.50")))
                .thenReturn(dto);

        ResponseEntity<ProductoDTO> response = controller.actualizarPrecioVenta(
                3, 10, new PrecioVentaClubRequestDTO(new java.math.BigDecimal("28.50")));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productoService).actualizarPrecioVentaEnClub(3, 10, new java.math.BigDecimal("28.50"));
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "n/a"));
    }

    private static Usuario usuario(int id, String rolNombre) {
        Rol rol = new Rol();
        rol.setNombre(rolNombre);
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        return usuario;
    }
}
