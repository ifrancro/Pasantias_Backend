package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.GlobalExceptionHandler;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoControllerRevisionTest {

    @Mock
    private ProductoService productoService;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ProductoController productoController;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rechazarSinComentarioPropaga400() {
        authenticateAs("ana@hub.com");
        when(usuarioRepository.findByEmail("ana@hub.com")).thenReturn(Optional.of(admin()));
        when(productoService.cambiarEstadoAprobacion(10, "RECHAZADO", null, 7))
                .thenThrow(new IllegalArgumentException("El comentario es obligatorio al rechazar un producto"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> productoController.cambiarEstadoAprobacion(10, "RECHAZADO", null));
        ResponseEntity<?> handled = new GlobalExceptionHandler().handleIllegalArgument(ex);
        assertEquals(HttpStatus.BAD_REQUEST, handled.getStatusCode());
    }

    @Test
    void reenviarProductoAjenoPropaga403() {
        authenticateAs("otro@club.com");
        Usuario otro = anfitrion(99, "otro@club.com");
        when(usuarioRepository.findByEmail("otro@club.com")).thenReturn(Optional.of(otro));
        when(productoService.reenviarProducto(10, 99))
                .thenThrow(new AccessDeniedException("No puedes reenviar un producto de otro club"));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> productoController.reenviarProducto(10));
        ResponseEntity<?> handled = new GlobalExceptionHandler().handleAccessDenied(ex);
        assertEquals(HttpStatus.FORBIDDEN, handled.getStatusCode());
    }

    @Test
    void reenviarLlamaAlServiceConUsuarioAutenticado() {
        authenticateAs("andrea@club.com");
        Usuario host = anfitrion(20, "andrea@club.com");
        when(usuarioRepository.findByEmail("andrea@club.com")).thenReturn(Optional.of(host));
        ProductoDTO dto = new ProductoDTO();
        dto.setId(10);
        dto.setEstadoAprobacion("PENDIENTE");
        when(productoService.reenviarProducto(10, 20)).thenReturn(dto);

        ResponseEntity<ProductoDTO> response = productoController.reenviarProducto(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("PENDIENTE", response.getBody().getEstadoAprobacion());
        verify(productoService).reenviarProducto(10, 20);
    }

    @Test
    void activarSinJwtDevuelve401() {
        ResponseEntity<ProductoDTO> response = productoController.activarProducto(10);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void desactivarSinJwtDevuelve401() {
        ResponseEntity<ProductoDTO> response = productoController.desactivarProducto(10);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void activarComoAnfitrionPropaga403() {
        authenticateAs("andrea@club.com");
        Usuario host = anfitrion(20, "andrea@club.com");
        when(usuarioRepository.findByEmail("andrea@club.com")).thenReturn(Optional.of(host));
        when(productoService.activarProducto(10, 20))
                .thenThrow(new AccessDeniedException("Solo un administrador puede activar o desactivar un producto"));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> productoController.activarProducto(10));
        ResponseEntity<?> handled = new GlobalExceptionHandler().handleAccessDenied(ex);
        assertEquals(HttpStatus.FORBIDDEN, handled.getStatusCode());
    }

    @Test
    void adminActivarPasaUsuarioId() {
        authenticateAs("ana@hub.com");
        when(usuarioRepository.findByEmail("ana@hub.com")).thenReturn(Optional.of(admin()));
        ProductoDTO dto = new ProductoDTO();
        dto.setActivo(true);
        when(productoService.activarProducto(10, 7)).thenReturn(dto);

        ResponseEntity<ProductoDTO> response = productoController.activarProducto(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productoService).activarProducto(10, 7);
    }

    @Test
    void adminDesactivarPasaUsuarioId() {
        authenticateAs("ana@hub.com");
        when(usuarioRepository.findByEmail("ana@hub.com")).thenReturn(Optional.of(admin()));
        ProductoDTO dto = new ProductoDTO();
        dto.setActivo(false);
        when(productoService.desactivarProducto(10, 7)).thenReturn(dto);

        ResponseEntity<ProductoDTO> response = productoController.desactivarProducto(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productoService).desactivarProducto(10, 7);
    }

    @Test
    void aprobarPasaComentarioOpcionalYAdminId() {
        authenticateAs("ana@hub.com");
        when(usuarioRepository.findByEmail("ana@hub.com")).thenReturn(Optional.of(admin()));
        ProductoDTO dto = new ProductoDTO();
        dto.setEstadoAprobacion("APROBADO");
        when(productoService.cambiarEstadoAprobacion(10, "APROBADO", null, 7)).thenReturn(dto);

        ResponseEntity<ProductoDTO> response =
                productoController.cambiarEstadoAprobacion(10, "APROBADO", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productoService).cambiarEstadoAprobacion(10, "APROBADO", null, 7);
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "n/a"));
    }

    private static Usuario admin() {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        Usuario usuario = new Usuario();
        usuario.setId(7);
        usuario.setEmail("ana@hub.com");
        usuario.setRol(rol);
        return usuario;
    }

    private static Usuario anfitrion(int id, String email) {
        Rol rol = new Rol();
        rol.setNombre("ANFITRION");
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setRol(rol);
        return usuario;
    }
}
