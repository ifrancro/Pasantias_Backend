package com.example.herbalife_clubes.controllers.auth;

import com.example.herbalife_clubes.dtos.auth.VerifyEmailRequest;
import com.example.herbalife_clubes.dtos.auth.VerifyEmailResponse;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.security.JwtService;
import com.example.herbalife_clubes.serviceimpls.AuthServiceImpl;
import com.example.herbalife_clubes.services.PasswordResetService;
import com.example.herbalife_clubes.services.VerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerVerifyEmailTest {

    @Mock
    private AuthServiceImpl authService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private VerificationService verificationService;
    @Mock
    private PasswordResetService passwordResetService;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    @Test
    void verifyEmailTrasRegisterBasicoActivaUsuarioYDevuelveJwt() {
        Rol rol = new Rol();
        rol.setNombre("USUARIO_BASICO");

        Usuario activo = new Usuario();
        activo.setId(7);
        activo.setEmail("basico-ok@test.com");
        activo.setNombre("Ana");
        activo.setApellido("Pérez");
        activo.setEstado("ACTIVO");
        activo.setRol(rol);

        when(verificationService.verifyCode("basico-ok@test.com", "123456"))
                .thenReturn(Optional.of(activo));
        when(jwtService.generateToken(activo)).thenReturn("jwt-tras-verify");

        ResponseEntity<?> response = authController.verifyEmail(
                new VerifyEmailRequest("basico-ok@test.com", "123456"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(VerifyEmailResponse.class, response.getBody());

        VerifyEmailResponse body = (VerifyEmailResponse) response.getBody();
        assertTrue(body.isVerified());
        assertEquals("jwt-tras-verify", body.getToken());
        assertEquals(7, body.getUserId());
        assertEquals("basico-ok@test.com", body.getEmail());
        assertEquals("USUARIO_BASICO", body.getRolNombre());

        verify(jwtService).generateToken(activo);
    }

    @Test
    void verifyEmailCodigoInvalidoNoGeneraJwt() {
        when(verificationService.verifyCode("basico-ok@test.com", "000000"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.verifyEmail(
                new VerifyEmailRequest("basico-ok@test.com", "000000"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(jwtService, never()).generateToken(any());
    }
}
