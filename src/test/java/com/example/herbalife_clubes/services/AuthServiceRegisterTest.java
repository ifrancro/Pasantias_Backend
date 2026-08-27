package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.auth.RegisterBasicoRequest;
import com.example.herbalife_clubes.dtos.auth.RegisterRequest;
import com.example.herbalife_clubes.dtos.auth.AuthenticationResponse;
import com.example.herbalife_clubes.dtos.auth.RegisterBasicoResponse;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.EmailAlreadyExistsException;
import com.example.herbalife_clubes.exceptions.EmailDeliveryException;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.security.JwtService;
import com.example.herbalife_clubes.serviceimpls.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

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
    void emailNuevoRegistraNormalizandoYEnviandoOtp() {
        when(usuarioRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("hash");
        Rol rol = new Rol();
        rol.setNombre("USUARIO_BASICO");
        when(rolRepository.findByNombre("USUARIO_BASICO")).thenReturn(Optional.of(rol));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(42);
            return u;
        });
        doNothing().when(verificationService).generateAndSendCode(any(Usuario.class));

        AuthenticationResponse response = authService.register(RegisterRequest.builder()
                .nombre("Ana")
                .apellido("Pérez")
                .email("  Nuevo@Test.com ")
                .password("secret1")
                .rolId(1)
                .telefono("+59170000000")
                .build());

        assertEquals(42, response.getUserId());
        assertEquals("nuevo@test.com", response.getEmail());
        assertTrue(response.isRequiresVerification());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("nuevo@test.com", captor.getValue().getEmail());
        verify(verificationService).generateAndSendCode(any(Usuario.class));
    }

    @Test
    void emailExistenteLanzaEmailAlreadyExistsYNoLlamaSave() {
        when(usuarioRepository.existsByEmail("ya@test.com")).thenReturn(true);

        EmailAlreadyExistsException ex = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(RegisterRequest.builder()
                        .nombre("Ana")
                        .apellido("Pérez")
                        .email("YA@test.com")
                        .password("secret1")
                        .rolId(1)
                        .telefono("+59170000000")
                        .build()));

        assertEquals(EmailAlreadyExistsException.DEFAULT_MESSAGE, ex.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(verificationService, never()).generateAndSendCode(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void raceConditionEnSaveMapeaAEmailAlreadyExists() {
        when(usuarioRepository.existsByEmail("race@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        Rol rol = new Rol();
        rol.setNombre("USUARIO_BASICO");
        when(rolRepository.findByNombre("USUARIO_BASICO")).thenReturn(Optional.of(rol));
        when(usuarioRepository.save(any(Usuario.class))).thenThrow(
                new DataIntegrityViolationException(
                        "could not execute statement; constraint [usuarios_email_key]; "
                                + "SQL [n/a]; duplicate key value violates unique constraint"));

        EmailAlreadyExistsException ex = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(RegisterRequest.builder()
                        .nombre("Ana")
                        .apellido("Pérez")
                        .email("race@test.com")
                        .password("secret1")
                        .rolId(1)
                        .telefono("+59170000000")
                        .build()));

        assertEquals(EmailAlreadyExistsException.DEFAULT_MESSAGE, ex.getMessage());
        assertFalse(ex.getMessage().toLowerCase().contains("sql"));
        assertFalse(ex.getMessage().toLowerCase().contains("usuarios_email"));
        verify(verificationService, never()).generateAndSendCode(any());
    }

    @Test
    void registerBasicoEmailExistenteDevuelve409YNoLlamaSave() {
        when(usuarioRepository.existsByEmail("basico@test.com")).thenReturn(true);

        EmailAlreadyExistsException ex = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.registerBasico(RegisterBasicoRequest.builder()
                        .nombre("Ana")
                        .apellido("Pérez")
                        .email("basico@test.com")
                        .password("secret1")
                        .telefono("+59170000000")
                        .build()));

        assertEquals(EmailAlreadyExistsException.DEFAULT_MESSAGE, ex.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(verificationService, never()).generateAndSendCode(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void registerBasicoUsuarioNuevoPendienteVerificacionSinJwt() {
        when(usuarioRepository.existsByEmail("basico-ok@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        Rol rol = new Rol();
        rol.setNombre("USUARIO_BASICO");
        when(rolRepository.findByNombre("USUARIO_BASICO")).thenReturn(Optional.of(rol));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(7);
            return u;
        });
        doNothing().when(verificationService).generateAndSendCode(any(Usuario.class));

        RegisterBasicoResponse response = authService.registerBasico(RegisterBasicoRequest.builder()
                .nombre("Ana")
                .apellido("Pérez")
                .email("Basico-Ok@Test.com")
                .password("secret1")
                .telefono("+59170000000")
                .build());

        assertEquals(7, response.getUserId());
        assertEquals("basico-ok@test.com", response.getEmail());
        assertEquals("Ana", response.getNombre());
        assertEquals("Pérez", response.getApellido());
        assertEquals("USUARIO_BASICO", response.getRolNombre());
        assertTrue(response.isRequiresVerification());
        assertNull(response.getToken());
        assertEquals("ACTIVATE:7", response.getQrActivacionPayload());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("basico-ok@test.com", captor.getValue().getEmail());
        assertEquals("PENDIENTE_VERIFICACION", captor.getValue().getEstado());
        verify(verificationService).generateAndSendCode(any(Usuario.class));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void registerBasicoRaceConditionEnSaveMapeaAEmailAlreadyExists() {
        when(usuarioRepository.existsByEmail("race-basico@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        Rol rol = new Rol();
        rol.setNombre("USUARIO_BASICO");
        when(rolRepository.findByNombre("USUARIO_BASICO")).thenReturn(Optional.of(rol));
        when(usuarioRepository.save(any(Usuario.class))).thenThrow(
                new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.registerBasico(RegisterBasicoRequest.builder()
                        .nombre("Ana")
                        .apellido("Pérez")
                        .email("race-basico@test.com")
                        .password("secret1")
                        .telefono("+59170000000")
                        .build()));

        verify(verificationService, never()).generateAndSendCode(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void registerFalloEntregaOtpEliminaUsuarioYPropagaEmailDeliveryException() {
        when(usuarioRepository.existsByEmail("fail@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        Rol rol = new Rol();
        rol.setNombre("USUARIO_BASICO");
        when(rolRepository.findByNombre("USUARIO_BASICO")).thenReturn(Optional.of(rol));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(99);
            return u;
        });
        doThrow(new EmailDeliveryException())
                .when(verificationService).generateAndSendCode(any(Usuario.class));

        EmailDeliveryException ex = assertThrows(
                EmailDeliveryException.class,
                () -> authService.register(RegisterRequest.builder()
                        .nombre("Ana")
                        .apellido("Pérez")
                        .email("fail@test.com")
                        .password("secret1")
                        .rolId(1)
                        .telefono("+59170000000")
                        .build()));

        assertEquals(EmailDeliveryException.DEFAULT_MESSAGE, ex.getMessage());
        verify(usuarioRepository).deleteById(99);
    }

    @Test
    void registerFalloEntregaYFallaCompensacionMantieneEmailDeliveryException() {
        when(usuarioRepository.existsByEmail("comp-fail@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        Rol rol = new Rol();
        rol.setNombre("USUARIO_BASICO");
        when(rolRepository.findByNombre("USUARIO_BASICO")).thenReturn(Optional.of(rol));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(77);
            return u;
        });
        doThrow(new EmailDeliveryException())
                .when(verificationService).generateAndSendCode(any(Usuario.class));
        doThrow(new RuntimeException("could not execute statement; FK verification_codes"))
                .when(usuarioRepository).deleteById(77);

        EmailDeliveryException ex = assertThrows(
                EmailDeliveryException.class,
                () -> authService.register(RegisterRequest.builder()
                        .nombre("Ana")
                        .apellido("Pérez")
                        .email("comp-fail@test.com")
                        .password("secret1")
                        .rolId(1)
                        .telefono("+59170000000")
                        .build()));

        assertEquals(EmailDeliveryException.DEFAULT_MESSAGE, ex.getMessage());
        verify(usuarioRepository).deleteById(77);
        assertFalse(ex.getMessage().contains("FK"));
        assertFalse(ex.getMessage().contains("verification_codes"));
        assertFalse(ex.getMessage().contains("could not execute"));
    }

    @Test
    void registerOtraExcepcionNoCompensaNiEtiquetaComoEmailDelivery() {
        when(usuarioRepository.existsByEmail("other@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        Rol rol = new Rol();
        rol.setNombre("USUARIO_BASICO");
        when(rolRepository.findByNombre("USUARIO_BASICO")).thenReturn(Optional.of(rol));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(50);
            return u;
        });
        doThrow(new RuntimeException("fallo interno de verificación"))
                .when(verificationService).generateAndSendCode(any(Usuario.class));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> authService.register(RegisterRequest.builder()
                        .nombre("Ana")
                        .apellido("Pérez")
                        .email("other@test.com")
                        .password("secret1")
                        .rolId(1)
                        .telefono("+59170000000")
                        .build()));

        assertEquals("fallo interno de verificación", ex.getMessage());
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    void registerBasicoFalloEntregaOtpEliminaUsuarioYPropagaEmailDeliveryException() {
        when(usuarioRepository.existsByEmail("basico-fail@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        Rol rol = new Rol();
        rol.setNombre("USUARIO_BASICO");
        when(rolRepository.findByNombre("USUARIO_BASICO")).thenReturn(Optional.of(rol));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(88);
            return u;
        });
        doThrow(new EmailDeliveryException())
                .when(verificationService).generateAndSendCode(any(Usuario.class));

        EmailDeliveryException ex = assertThrows(
                EmailDeliveryException.class,
                () -> authService.registerBasico(RegisterBasicoRequest.builder()
                        .nombre("Ana")
                        .apellido("Pérez")
                        .email("basico-fail@test.com")
                        .password("secret1")
                        .telefono("+59170000000")
                        .build()));

        assertEquals(EmailDeliveryException.DEFAULT_MESSAGE, ex.getMessage());
        verify(usuarioRepository).deleteById(88);
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void normalizeEmailTrimYLowercase() {
        assertEquals("a@b.com", AuthServiceImpl.normalizeEmail("  A@B.com "));
        assertNull(AuthServiceImpl.normalizeEmail(null));
    }
}
