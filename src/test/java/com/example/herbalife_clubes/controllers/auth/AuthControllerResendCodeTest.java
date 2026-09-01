package com.example.herbalife_clubes.controllers.auth;

import com.example.herbalife_clubes.dtos.auth.ResendCodeRequest;
import com.example.herbalife_clubes.exceptions.EmailDeliveryException;
import com.example.herbalife_clubes.exceptions.OtpResendCooldownException;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
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

import jakarta.mail.MessagingException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerResendCodeTest {

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
    void resendCodeFalloEntregaPropagaEmailDeliveryExceptionParaHandler503() {
        doThrow(new EmailDeliveryException(
                new MessagingException("Authentication failed smtp-relay.brevo.com secret=XYZ")))
                .when(verificationService).resendCode("user@test.com");

        EmailDeliveryException ex = assertThrows(
                EmailDeliveryException.class,
                () -> authController.resendCode(new ResendCodeRequest("user@test.com")));

        assertEquals(EmailDeliveryException.DEFAULT_MESSAGE, ex.getMessage());
    }

    @Test
    void resendCodeUsuarioNoEncontradoDevuelve400() {
        doThrow(new ResourceNotFoundException(
                "Usuario no encontrado con email: missing@test.com"))
                .when(verificationService).resendCode("missing@test.com");

        ResponseEntity<?> response =
                authController.resendCode(new ResendCodeRequest("missing@test.com"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("Usuario no encontrado con email: missing@test.com", body.get("message"));
    }

    @Test
    void resendCodeCooldownDevuelve429ConRetryAfterSeconds() {
        doThrow(new OtpResendCooldownException(37))
                .when(verificationService).resendCode("cool@test.com");

        ResponseEntity<?> response =
                authController.resendCode(new ResendCodeRequest("cool@test.com"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals(OtpResendCooldownException.ERROR_CODE, body.get("error"));
        assertEquals(OtpResendCooldownException.MESSAGE, body.get("message"));
        assertEquals(37, body.get("retryAfterSeconds"));
    }

    @Test
    void resendCodeLimiteReenviosSigueDevolviendo400() {
        String rateLimitMessage =
                "Has excedido el límite de reenvíos (5). Intenta de nuevo en 24 horas.";
        doThrow(new RuntimeException(rateLimitMessage))
                .when(verificationService).resendCode("limit@test.com");

        ResponseEntity<?> response =
                authController.resendCode(new ResendCodeRequest("limit@test.com"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals(rateLimitMessage, body.get("message"));
    }

    @Test
    void resendCodeRuntimeTecnicaDevuelve500SinDetallesInternos() {
        doThrow(new RuntimeException(
                "could not execute statement PostgreSQL password=SECRET"))
                .when(verificationService).resendCode("tech@test.com");

        ResponseEntity<?> response =
                authController.resendCode(new ResendCodeRequest("tech@test.com"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("No se pudo reenviar el código. Inténtalo nuevamente.", body.get("message"));

        String serialized = String.valueOf(body).toLowerCase();
        assertFalse(serialized.contains("postgresql"));
        assertFalse(serialized.contains("password"));
        assertFalse(serialized.contains("secret"));
        assertFalse(serialized.contains("could not execute statement"));
    }

    @Test
    void resendCodeExitosoDevuelve200() {
        doNothing().when(verificationService).resendCode("ok@test.com");

        ResponseEntity<?> response =
                authController.resendCode(new ResendCodeRequest("ok@test.com"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
