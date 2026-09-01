package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.entities.VerificationCodePurpose;
import com.example.herbalife_clubes.exceptions.EmailDeliveryException;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.repositories.VerificationCodeRepository;
import com.example.herbalife_clubes.serviceimpls.VerificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AUTH-005: el rollback transaccional que preserva el OTP anterior ante fallo de envío
 * requiere contexto Spring @Transactional real (test de integración).
 * Aquí solo verificamos propagación de EmailDeliveryException y rate limit intacto.
 */
@ExtendWith(MockitoExtension.class)
class VerificationServiceResendDeliveryTest {

    @Mock
    private VerificationCodeRepository verificationCodeRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    @BeforeEach
    void configureDefaults() {
        ReflectionTestUtils.setField(verificationService, "codeLength", 6);
        ReflectionTestUtils.setField(verificationService, "expirationMinutes", 15);
        ReflectionTestUtils.setField(verificationService, "maxResends", 5);
        ReflectionTestUtils.setField(verificationService, "resendCooldownSeconds", 60);
    }

    @Test
    void resendCodePropagaEmailDeliveryExceptionSinConvertirEnRuntimeGenerico() {
        Usuario usuario = new Usuario();
        usuario.setId(3);
        usuario.setEmail("pending@test.com");
        usuario.setNombre("Ana");

        when(usuarioRepository.findByEmailForUpdate("pending@test.com")).thenReturn(Optional.of(usuario));
        when(verificationCodeRepository.findTopByUsuarioAndPurposeOrderByCreatedAtDesc(
                usuario, VerificationCodePurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());
        when(verificationCodeRepository.countRecentCodes(
                eq(usuario), eq(VerificationCodePurpose.EMAIL_VERIFICATION), any(LocalDateTime.class)))
                .thenReturn(1L);
        doNothing().when(verificationCodeRepository)
                .invalidateAllByUsuarioAndPurpose(usuario, VerificationCodePurpose.EMAIL_VERIFICATION);
        when(verificationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new EmailDeliveryException())
                .when(emailService).sendVerificationCode(anyString(), anyString(), anyString());

        EmailDeliveryException ex = assertThrows(
                EmailDeliveryException.class,
                () -> verificationService.resendCode("pending@test.com"));

        assertEquals(EmailDeliveryException.DEFAULT_MESSAGE, ex.getMessage());
    }

    @Test
    void resendCodeRateLimitSigueLanzandoRuntimeExceptionAntesDeEnviar() {
        Usuario usuario = new Usuario();
        usuario.setId(4);
        usuario.setEmail("limit@test.com");

        when(usuarioRepository.findByEmailForUpdate("limit@test.com")).thenReturn(Optional.of(usuario));
        when(verificationCodeRepository.findTopByUsuarioAndPurposeOrderByCreatedAtDesc(
                usuario, VerificationCodePurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());
        when(verificationCodeRepository.countRecentCodes(
                eq(usuario), eq(VerificationCodePurpose.EMAIL_VERIFICATION), any(LocalDateTime.class)))
                .thenReturn(5L);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> verificationService.resendCode("limit@test.com"));

        assertTrue(ex.getMessage().contains("límite de reenvíos"));
        verify(emailService, never()).sendVerificationCode(anyString(), anyString(), anyString());
        verify(verificationCodeRepository, never()).save(any());
    }
}
