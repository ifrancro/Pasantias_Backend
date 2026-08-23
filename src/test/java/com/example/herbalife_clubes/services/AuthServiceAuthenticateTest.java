package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.auth.AuthenticationRequest;
import com.example.herbalife_clubes.dtos.auth.AuthenticationResponse;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.EmailNotVerifiedException;
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
class AuthServiceAuthenticateTest {

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
    void activoConPasswordCorrectaDevuelveJwt() {
        Usuario usuario = usuarioConEstado("ACTIVO", "hash");
        when(usuarioRepository.findByEmail("activo@test.com"))
                .thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("activo@test.com", "secret"));
        when(jwtService.generateToken(usuario)).thenReturn("jwt-ok");

        AuthenticationResponse response = authService.authenticate(
                AuthenticationRequest.builder()
                        .email("activo@test.com")
                        .password("secret")
                        .build());

        assertEquals("jwt-ok", response.getToken());
        assertEquals("activo@test.com", response.getEmail());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void pendienteConPasswordCorrectaLanzaEmailNotVerified() {
        Usuario usuario = usuarioConEstado("PENDIENTE_VERIFICACION", "hash");
        when(usuarioRepository.findByEmail("pendiente@test.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

        EmailNotVerifiedException ex = assertThrows(
                EmailNotVerifiedException.class,
                () -> authService.authenticate(AuthenticationRequest.builder()
                        .email("pendiente@test.com")
                        .password("secret")
                        .build()));

        assertEquals("EMAIL_NOT_VERIFIED", EmailNotVerifiedException.ERROR_CODE);
        assertTrue(ex.getMessage().toLowerCase().contains("verificar"));
        verify(authenticationManager, never()).authenticate(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void pendienteConPasswordIncorrectaLanzaBadCredentials() {
        Usuario usuario = usuarioConEstado("PENDIENTE_VERIFICACION", "hash");
        when(usuarioRepository.findByEmail("pendiente@test.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.authenticate(AuthenticationRequest.builder()
                        .email("pendiente@test.com")
                        .password("wrong")
                        .build()));

        verify(authenticationManager, never()).authenticate(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void usuarioDeshabilitadoNoSeConfundeConPendiente() {
        // Estado distinto de PENDIENTE_VERIFICACION: sigue el AuthenticationManager
        // (DisabledException vía isEnabled=false).
        Usuario usuario = usuarioConEstado("INACTIVO", "hash");
        when(usuarioRepository.findByEmail("off@test.com"))
                .thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        assertThrows(
                DisabledException.class,
                () -> authService.authenticate(AuthenticationRequest.builder()
                        .email("off@test.com")
                        .password("secret")
                        .build()));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    private static Usuario usuarioConEstado(String estado, String passwordHash) {
        Rol rol = new Rol();
        rol.setId(4);
        rol.setNombre("USUARIO_BASICO");
        return Usuario.builder()
                .id(10)
                .email(estado.toLowerCase().contains("pendiente")
                        ? "pendiente@test.com"
                        : estado.equalsIgnoreCase("ACTIVO")
                            ? "activo@test.com"
                            : "off@test.com")
                .nombre("Test")
                .apellido("User")
                .passwordHash(passwordHash)
                .estado(estado)
                .rol(rol)
                .build();
    }
}
