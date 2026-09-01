package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.entities.VerificationCode;
import com.example.herbalife_clubes.entities.VerificationCodePurpose;
import com.example.herbalife_clubes.exceptions.OtpResendCooldownException;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.repositories.VerificationCodeRepository;
import com.example.herbalife_clubes.serviceimpls.VerificationServiceImpl;
import com.example.herbalife_clubes.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceResendCooldownTest {

    @Mock
    private VerificationCodeRepository verificationCodeRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(verificationService, "codeLength", 6);
        ReflectionTestUtils.setField(verificationService, "expirationMinutes", 15);
        ReflectionTestUtils.setField(verificationService, "maxResends", 5);
        ReflectionTestUtils.setField(verificationService, "resendCooldownSeconds", 60);

        usuario = new Usuario();
        usuario.setId(10);
        usuario.setEmail("user@test.com");
        usuario.setNombre("User");
    }

    @Test
    void primerResendSinCodigoPrevioPermitido() {
        when(usuarioRepository.findByEmailForUpdate("user@test.com")).thenReturn(Optional.of(usuario));
        when(verificationCodeRepository.findTopByUsuarioAndPurposeOrderByCreatedAtDesc(
                usuario, VerificationCodePurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());
        when(verificationCodeRepository.countRecentCodes(
                eq(usuario), eq(VerificationCodePurpose.EMAIL_VERIFICATION), any(LocalDateTime.class)))
                .thenReturn(0L);
        doNothing().when(verificationCodeRepository)
                .invalidateAllByUsuarioAndPurpose(usuario, VerificationCodePurpose.EMAIL_VERIFICATION);
        when(verificationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendVerificationCode(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> verificationService.resendCode("user@test.com"));

        verify(emailService).sendVerificationCode(eq("user@test.com"), anyString(), anyString());
        verify(verificationCodeRepository).save(any(VerificationCode.class));
    }

    @Test
    void segundoResendInmediatoLanzaCooldown429() {
        VerificationCode last = VerificationCode.builder()
                .usuario(usuario)
                .code("111111")
                .purpose(VerificationCodePurpose.EMAIL_VERIFICATION)
                .createdAt(LocalDateTime.now().minusSeconds(23))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        when(usuarioRepository.findByEmailForUpdate("user@test.com")).thenReturn(Optional.of(usuario));
        when(verificationCodeRepository.findTopByUsuarioAndPurposeOrderByCreatedAtDesc(
                usuario, VerificationCodePurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(last));

        OtpResendCooldownException ex = assertThrows(
                OtpResendCooldownException.class,
                () -> verificationService.resendCode("user@test.com"));

        assertEquals(OtpResendCooldownException.ERROR_CODE, OtpResendCooldownException.ERROR_CODE);
        assertEquals(37, ex.getRetryAfterSeconds());
        verify(verificationCodeRepository, never()).save(any());
        verify(verificationCodeRepository, never())
                .invalidateAllByUsuarioAndPurpose(any(), any());
        verify(emailService, never()).sendVerificationCode(anyString(), anyString(), anyString());
    }

    @Test
    void retryAfterSecondsMinimoEsUnoDentroDeCooldown() {
        VerificationCode last = VerificationCode.builder()
                .usuario(usuario)
                .code("111111")
                .purpose(VerificationCodePurpose.EMAIL_VERIFICATION)
                .createdAt(LocalDateTime.now().minusSeconds(59))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        when(usuarioRepository.findByEmailForUpdate("user@test.com")).thenReturn(Optional.of(usuario));
        when(verificationCodeRepository.findTopByUsuarioAndPurposeOrderByCreatedAtDesc(
                usuario, VerificationCodePurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(last));

        OtpResendCooldownException ex = assertThrows(
                OtpResendCooldownException.class,
                () -> verificationService.resendCode("user@test.com"));

        assertEquals(1, ex.getRetryAfterSeconds());
    }

    @Test
    void trasSuperarCooldownPermiteResend() {
        VerificationCode last = VerificationCode.builder()
                .usuario(usuario)
                .code("111111")
                .purpose(VerificationCodePurpose.EMAIL_VERIFICATION)
                .createdAt(LocalDateTime.now().minusSeconds(61))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        when(usuarioRepository.findByEmailForUpdate("user@test.com")).thenReturn(Optional.of(usuario));
        when(verificationCodeRepository.findTopByUsuarioAndPurposeOrderByCreatedAtDesc(
                usuario, VerificationCodePurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(last));
        when(verificationCodeRepository.countRecentCodes(
                eq(usuario), eq(VerificationCodePurpose.EMAIL_VERIFICATION), any(LocalDateTime.class)))
                .thenReturn(1L);
        doNothing().when(verificationCodeRepository)
                .invalidateAllByUsuarioAndPurpose(usuario, VerificationCodePurpose.EMAIL_VERIFICATION);
        when(verificationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendVerificationCode(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> verificationService.resendCode("user@test.com"));

        verify(emailService).sendVerificationCode(anyString(), anyString(), anyString());
    }

    @Test
    void resendExitosoInvalidaAnterior() {
        when(usuarioRepository.findByEmailForUpdate("user@test.com")).thenReturn(Optional.of(usuario));
        when(verificationCodeRepository.findTopByUsuarioAndPurposeOrderByCreatedAtDesc(
                usuario, VerificationCodePurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());
        when(verificationCodeRepository.countRecentCodes(
                eq(usuario), eq(VerificationCodePurpose.EMAIL_VERIFICATION), any(LocalDateTime.class)))
                .thenReturn(0L);
        doNothing().when(verificationCodeRepository)
                .invalidateAllByUsuarioAndPurpose(usuario, VerificationCodePurpose.EMAIL_VERIFICATION);
        when(verificationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendVerificationCode(anyString(), anyString(), anyString());

        verificationService.resendCode("user@test.com");

        verify(verificationCodeRepository).invalidateAllByUsuarioAndPurpose(
                usuario, VerificationCodePurpose.EMAIL_VERIFICATION);
        ArgumentCaptor<VerificationCode> captor = ArgumentCaptor.forClass(VerificationCode.class);
        verify(verificationCodeRepository).save(captor.capture());
        assertEquals(VerificationCodePurpose.EMAIL_VERIFICATION, captor.getValue().getPurpose());
    }

    @Test
    void limiteDiarioSigueFuncionando() {
        VerificationCode last = VerificationCode.builder()
                .usuario(usuario)
                .code("111111")
                .purpose(VerificationCodePurpose.EMAIL_VERIFICATION)
                .createdAt(LocalDateTime.now().minusSeconds(120))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        when(usuarioRepository.findByEmailForUpdate("user@test.com")).thenReturn(Optional.of(usuario));
        when(verificationCodeRepository.findTopByUsuarioAndPurposeOrderByCreatedAtDesc(
                usuario, VerificationCodePurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(last));
        when(verificationCodeRepository.countRecentCodes(
                eq(usuario), eq(VerificationCodePurpose.EMAIL_VERIFICATION), any(LocalDateTime.class)))
                .thenReturn(5L);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> verificationService.resendCode("user@test.com"));

        assertTrue(ex.getMessage().contains("límite de reenvíos"));
        verify(emailService, never()).sendVerificationCode(anyString(), anyString(), anyString());
    }

    @Test
    void generateAndSendCodeInicialNoEvaluaCooldown() {
        when(verificationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(verificationCodeRepository)
                .invalidateAllByUsuarioAndPurpose(usuario, VerificationCodePurpose.EMAIL_VERIFICATION);
        doNothing().when(emailService).sendVerificationCode(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> verificationService.generateAndSendCode(usuario));

        verify(verificationCodeRepository, never())
                .findTopByUsuarioAndPurposeOrderByCreatedAtDesc(any(), any());
        verify(emailService).sendVerificationCode(anyString(), anyString(), anyString());
    }
}
