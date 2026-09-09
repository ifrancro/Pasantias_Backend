package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.usuario.UsuarioDTO;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ConflictException;
import com.example.herbalife_clubes.exceptions.GlobalExceptionHandler;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.SocioActivationService;
import com.example.herbalife_clubes.services.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerEstadoTest {

    private static final Integer ADMIN_ID = 1;
    private static final Integer USUARIO_ID = 42;
    private static final String ADMIN_EMAIL = "admin@demo.com";

    @Mock
    private UsuarioService usuarioService;
    @Mock
    private SocioActivationService socioActivationService;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioController usuarioController;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void desactivarPasaElIdDelAdminAutenticadoAlServicio() {
        autenticarComoAdmin();
        when(usuarioService.desactivarUsuario(USUARIO_ID, ADMIN_ID)).thenReturn(dto("INACTIVO"));

        ResponseEntity<UsuarioDTO> response = usuarioController.desactivarUsuario(USUARIO_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INACTIVO", response.getBody().getEstado());
        verify(usuarioService).desactivarUsuario(USUARIO_ID, ADMIN_ID);
    }

    @Test
    void reactivarDevuelve200ConElUsuarioActivo() {
        when(usuarioService.reactivarUsuario(USUARIO_ID)).thenReturn(dto("ACTIVO"));

        ResponseEntity<UsuarioDTO> response = usuarioController.reactivarUsuario(USUARIO_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACTIVO", response.getBody().getEstado());
        verify(usuarioService).reactivarUsuario(USUARIO_ID);
    }

    @Test
    void desactivarSinContextoDeSeguridadEs401() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioController.desactivarUsuario(USUARIO_ID));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void conflictoDeTransicionSeTraduceEn409() {
        when(usuarioService.reactivarUsuario(USUARIO_ID))
                .thenThrow(new ConflictException("El usuario ya esta activo"));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> usuarioController.reactivarUsuario(USUARIO_ID));

        ResponseEntity<?> handled = new GlobalExceptionHandler().handleConflict(ex);
        assertEquals(HttpStatus.CONFLICT, handled.getStatusCode());
    }

    @Test
    void ambosEndpointsDeEstadoSonPatchYSoloAdmin() throws Exception {
        assertPatchSoloAdmin("desactivarUsuario", "{id}/desactivar");
        assertPatchSoloAdmin("reactivarUsuario", "{id}/reactivar");
    }

    private void assertPatchSoloAdmin(String metodo, String ruta) throws Exception {
        Method m = UsuarioController.class.getDeclaredMethod(metodo, Integer.class);

        PatchMapping mapping = m.getAnnotation(PatchMapping.class);
        assertNotNull(mapping, metodo + " debe exponerse como PATCH");
        assertEquals(ruta, mapping.value()[0]);

        PreAuthorize preAuthorize = m.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize,
                metodo + " debe estar restringido a ADMIN: sin @PreAuthorize cualquier usuario "
                        + "autenticado puede cambiar el estado de otras cuentas");
        assertEquals("hasRole('ADMIN')", preAuthorize.value());
    }

    private void autenticarComoAdmin() {
        Usuario admin = new Usuario();
        admin.setId(ADMIN_ID);
        admin.setEmail(ADMIN_EMAIL);
        when(usuarioRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ADMIN_EMAIL, "n/a"));
    }

    private UsuarioDTO dto(String estado) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(USUARIO_ID);
        dto.setEstado(estado);
        return dto;
    }
}
