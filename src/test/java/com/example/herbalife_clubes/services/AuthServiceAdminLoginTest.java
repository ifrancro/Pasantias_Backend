package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.auth.AdminLoginResponse;
import com.example.herbalife_clubes.dtos.auth.AuthenticationRequest;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.AdminAccessDeniedException;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.security.JwtService;
import com.example.herbalife_clubes.serviceimpls.AuthServiceImpl;
import com.example.herbalife_clubes.services.VerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceAdminLoginTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private VerificationService verificationService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void adminValidoDevuelve200ConToken() {
        Usuario admin = usuarioConRol("ADMIN", "ACTIVO", "admin@demo.com");
        when(usuarioRepository.findByEmail("admin@demo.com")).thenReturn(Optional.of(admin));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("admin@demo.com", "secret"));
        when(jwtService.generateToken(admin)).thenReturn("jwt-admin");

        AdminLoginResponse response = authService.authenticateAdmin(
                AuthenticationRequest.builder()
                        .email("  Admin@Demo.com ")
                        .password("secret")
                        .build());

        assertEquals("jwt-admin", response.getToken());
        assertEquals(1, response.getUserId());
        assertEquals("admin@demo.com", response.getEmail());
        assertEquals("Admin", response.getNombre());
        assertEquals("Corporativo", response.getApellido());
        assertEquals("ADMIN", response.getRolNombre());
        verify(jwtService).generateToken(admin);
    }

    @Test
    void socioValidoNoRecibeTokenNiSeGeneraJwt() {
        assertNoAdminTokenForRole("SOCIO");
    }

    @Test
    void anfitrionValidoNoRecibeTokenNiSeGeneraJwt() {
        assertNoAdminTokenForRole("ANFITRION");
    }

    @Test
    void usuarioBasicoValidoNoRecibeTokenNiSeGeneraJwt() {
        assertNoAdminTokenForRole("USUARIO_BASICO");
    }

    @Test
    void passwordIncorrectaLanzaBadCredentialsYNoGeneraJwt() {
        when(usuarioRepository.findByEmail("admin@demo.com")).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.authenticateAdmin(AuthenticationRequest.builder()
                        .email("admin@demo.com")
                        .password("wrong")
                        .build()));

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void adminDeshabilitadoLanzaDisabledYNoGeneraJwt() {
        when(usuarioRepository.findByEmail("admin@demo.com")).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        assertThrows(
                DisabledException.class,
                () -> authService.authenticateAdmin(AuthenticationRequest.builder()
                        .email("admin@demo.com")
                        .password("secret")
                        .build()));

        verify(jwtService, never()).generateToken(any());
    }

    private void assertNoAdminTokenForRole(String rolNombre) {
        Usuario usuario = usuarioConRol(rolNombre, "ACTIVO", "user@demo.com");
        when(usuarioRepository.findByEmail("user@demo.com")).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("user@demo.com", "secret"));

        AdminAccessDeniedException ex = assertThrows(
                AdminAccessDeniedException.class,
                () -> authService.authenticateAdmin(AuthenticationRequest.builder()
                        .email("user@demo.com")
                        .password("secret")
                        .build()));

        assertEquals(AdminAccessDeniedException.DEFAULT_MESSAGE, ex.getMessage());
        verify(jwtService, never()).generateToken(any());
    }

    private static Usuario usuarioConRol(String rolNombre, String estado, String email) {
        Rol rol = new Rol();
        rol.setId(1);
        rol.setNombre(rolNombre);
        return Usuario.builder()
                .id(1)
                .email(email)
                .nombre("Admin".equals(rolNombre) || "ADMIN".equals(rolNombre) ? "Admin" : "User")
                .apellido("ADMIN".equals(rolNombre) ? "Corporativo" : "Demo")
                .passwordHash("hash")
                .estado(estado)
                .rol(rol)
                .build();
    }
}
