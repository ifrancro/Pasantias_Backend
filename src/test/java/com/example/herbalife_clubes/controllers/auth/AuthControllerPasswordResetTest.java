package com.example.herbalife_clubes.controllers.auth;

import com.example.herbalife_clubes.dtos.auth.ForgotPasswordRequest;
import com.example.herbalife_clubes.dtos.auth.ResetPasswordRequest;
import com.example.herbalife_clubes.dtos.auth.VerifyResetCodeRequest;
import com.example.herbalife_clubes.exceptions.ResetCodeInvalidException;
import com.example.herbalife_clubes.exceptions.ResetTokenInvalidException;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.security.JwtService;
import com.example.herbalife_clubes.serviceimpls.AuthServiceImpl;
import com.example.herbalife_clubes.services.PasswordResetService;
import com.example.herbalife_clubes.services.VerificationService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerPasswordResetTest {

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

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void forgotPasswordDevuelve200Generico() {
        doNothing().when(passwordResetService).requestPasswordReset("user@test.com");

        ResponseEntity<Map<String, Object>> response = authController.forgotPassword(
                new ForgotPasswordRequest("user@test.com"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals(PasswordResetService.FORGOT_PASSWORD_PUBLIC_MESSAGE, response.getBody().get("message"));
    }

    @Test
    void forgotPasswordEmailInexistenteYFalloEnvioDevuelvenMismo200Generico() {
        doNothing().when(passwordResetService).requestPasswordReset("missing@test.com");
        doNothing().when(passwordResetService).requestPasswordReset("exists@test.com");

        ResponseEntity<Map<String, Object>> missing = authController.forgotPassword(
                new ForgotPasswordRequest("missing@test.com"));
        ResponseEntity<Map<String, Object>> exists = authController.forgotPassword(
                new ForgotPasswordRequest("exists@test.com"));

        assertEquals(HttpStatus.OK, missing.getStatusCode());
        assertEquals(HttpStatus.OK, exists.getStatusCode());
        assertEquals(missing.getBody(), exists.getBody());
        assertEquals(true, exists.getBody().get("success"));
        assertEquals(PasswordResetService.FORGOT_PASSWORD_PUBLIC_MESSAGE, exists.getBody().get("message"));
    }

    @Test
    void verifyResetCodeValidoDevuelveResetToken() {
        when(passwordResetService.verifyResetCode("user@test.com", "123456"))
                .thenReturn("opaque-reset-token");

        ResponseEntity<Map<String, Object>> response = authController.verifyResetCode(
                new VerifyResetCodeRequest("user@test.com", "123456"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("opaque-reset-token", response.getBody().get("resetToken"));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void verifyResetCodeInvalidoDevuelve400Controlado() {
        when(passwordResetService.verifyResetCode("user@test.com", "000000"))
                .thenThrow(new ResetCodeInvalidException());

        ResponseEntity<Map<String, Object>> response = authController.verifyResetCode(
                new VerifyResetCodeRequest("user@test.com", "000000"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertEquals(ResetCodeInvalidException.ERROR_CODE, response.getBody().get("error"));
        assertEquals(ResetCodeInvalidException.MESSAGE, response.getBody().get("message"));
    }

    @Test
    void resetPasswordValidoDevuelve200() {
        doNothing().when(passwordResetService)
                .resetPassword("token-abc", "NewPass123!");

        ResponseEntity<Map<String, Object>> response = authController.resetPassword(
                new ResetPasswordRequest("token-abc", "NewPass123!"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("Contraseña actualizada correctamente.", response.getBody().get("message"));
    }

    @Test
    void resetPasswordTokenInvalidoDevuelve400Controlado() {
        doThrow(new ResetTokenInvalidException())
                .when(passwordResetService).resetPassword("bad-token", "NewPass123!");

        ResponseEntity<Map<String, Object>> response = authController.resetPassword(
                new ResetPasswordRequest("bad-token", "NewPass123!"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertEquals(ResetTokenInvalidException.ERROR_CODE, response.getBody().get("error"));
        assertEquals(ResetTokenInvalidException.MESSAGE, response.getBody().get("message"));
    }

    @Test
    void resetPasswordMenorA8CaracteresFallaValidacion() {
        ResetPasswordRequest request = new ResetPasswordRequest("token-abc", "short1");

        Set<?> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.toString().contains("8 caracteres")));
    }
}
