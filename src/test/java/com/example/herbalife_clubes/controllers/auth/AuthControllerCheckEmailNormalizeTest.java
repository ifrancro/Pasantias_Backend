package com.example.herbalife_clubes.controllers.auth;

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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerCheckEmailNormalizeTest {

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
    void checkEmailNormalizaAntesDeConsultar() {
        when(usuarioRepository.existsByEmailIgnoreCase("socio1@demo.com")).thenReturn(true);

        ResponseEntity<?> response = authController.checkEmail("  SOCIO1@DEMO.COM  ");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(true, body.get("exists"));
        verify(usuarioRepository).existsByEmailIgnoreCase("socio1@demo.com");
    }

    @Test
    void checkEmailMixedCaseUsaExistsIgnoreCase() {
        when(usuarioRepository.existsByEmailIgnoreCase("evis96568@gmail.com")).thenReturn(true);

        ResponseEntity<?> response = authController.checkEmail("Evis96568@Gmail.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(true, body.get("exists"));
        verify(usuarioRepository).existsByEmailIgnoreCase("evis96568@gmail.com");
    }
}
