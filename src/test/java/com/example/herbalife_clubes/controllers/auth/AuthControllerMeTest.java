package com.example.herbalife_clubes.controllers.auth;

import com.example.herbalife_clubes.dtos.auth.MeResponse;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.security.JwtService;
import com.example.herbalife_clubes.serviceimpls.AuthServiceImpl;
import com.example.herbalife_clubes.services.VerificationService;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerMeTest {

    @Mock
    private AuthServiceImpl authService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private VerificationService verificationService;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void meAutenticadoDevuelve200ConMeResponse() {
        authenticateAs("andrea@test.com");

        Rol rol = new Rol();
        rol.setId(3);
        rol.setNombre("ANFITRION");

        Usuario usuario = new Usuario();
        usuario.setId(2);
        usuario.setNombre("Andrea");
        usuario.setApellido("Anfitriona");
        usuario.setEmail("andrea@test.com");
        usuario.setTelefono("+59171111111");
        usuario.setEstado("ACTIVO");
        usuario.setPasswordHash("no-debe-salir");
        usuario.setRol(rol);

        when(usuarioRepository.findByEmail("andrea@test.com")).thenReturn(Optional.of(usuario));

        ResponseEntity<?> response = authController.getAuthenticatedUser();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(MeResponse.class, response.getBody());

        MeResponse body = (MeResponse) response.getBody();
        assertEquals(2, body.getUserId());
        assertEquals("Andrea", body.getNombre());
        assertEquals("Anfitriona", body.getApellido());
        assertEquals("andrea@test.com", body.getEmail());
        assertEquals("+59171111111", body.getTelefono());
        assertEquals("ANFITRION", body.getRolNombre());
        assertEquals("ACTIVO", body.getEstado());
    }

    @Test
    void meConRolSocioDevuelveRolNombreCorrecto() {
        authenticateAs("socio@test.com");

        Rol rol = new Rol();
        rol.setNombre("SOCIO");
        Usuario usuario = new Usuario();
        usuario.setId(10);
        usuario.setEmail("socio@test.com");
        usuario.setNombre("Socio");
        usuario.setApellido("Uno");
        usuario.setEstado("ACTIVO");
        usuario.setRol(rol);

        when(usuarioRepository.findByEmail("socio@test.com")).thenReturn(Optional.of(usuario));

        MeResponse body = (MeResponse) authController.getAuthenticatedUser().getBody();
        assertNotNull(body);
        assertEquals("SOCIO", body.getRolNombre());
    }

    @Test
    void meConUsuarioBasicoDevuelveRolNombreCorrecto() {
        authenticateAs("basico@test.com");

        Rol rol = new Rol();
        rol.setNombre("USUARIO_BASICO");
        Usuario usuario = new Usuario();
        usuario.setId(11);
        usuario.setEmail("basico@test.com");
        usuario.setNombre("Basico");
        usuario.setApellido("Uno");
        usuario.setEstado("ACTIVO");
        usuario.setRol(rol);

        when(usuarioRepository.findByEmail("basico@test.com")).thenReturn(Optional.of(usuario));

        MeResponse body = (MeResponse) authController.getAuthenticatedUser().getBody();
        assertNotNull(body);
        assertEquals("USUARIO_BASICO", body.getRolNombre());
    }

    @Test
    void meUsuarioInexistenteDevuelve404Controlado() {
        authenticateAs("ghost@test.com");
        when(usuarioRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.getAuthenticatedUser();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("Usuario no encontrado", body.get("message"));
    }

    @Test
    void meSinAutenticacionDevuelve401() {
        SecurityContextHolder.clearContext();

        ResponseEntity<?> response = authController.getAuthenticatedUser();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("Usuario no autenticado", body.get("message"));
    }

    private static void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }
}
