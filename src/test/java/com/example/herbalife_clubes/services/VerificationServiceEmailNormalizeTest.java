package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.entities.VerificationCode;
import com.example.herbalife_clubes.entities.VerificationCodePurpose;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.repositories.VerificationCodeRepository;
import com.example.herbalife_clubes.services.EmailService;
import com.example.herbalife_clubes.serviceimpls.VerificationServiceImpl;
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
class VerificationServiceEmailNormalizeTest {

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
    void verifyCodeNormalizaEmailAntesDeBuscar() {
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setEmail("socio1@demo.com");
        usuario.setEstado("PENDIENTE_VERIFICACION");

        VerificationCode vc = VerificationCode.builder()
                .usuario(usuario)
                .code("123456")
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(verificationCodeRepository.findValidCode(
                eq("socio1@demo.com"), eq("123456"),
                eq(VerificationCodePurpose.EMAIL_VERIFICATION), any(LocalDateTime.class)))
                .thenReturn(Optional.of(vc));
        when(verificationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<Usuario> result =
                verificationService.verifyCode("  SOCIO1@DEMO.COM  ", "123456");

        assertTrue(result.isPresent());
        assertEquals("ACTIVO", result.get().getEstado());
        verify(verificationCodeRepository).findValidCode(
                eq("socio1@demo.com"), eq("123456"),
                eq(VerificationCodePurpose.EMAIL_VERIFICATION), any(LocalDateTime.class));
    }

    @Test
    void verifyCodeNoAceptaOtpPasswordReset() {
        when(verificationCodeRepository.findValidCode(
                eq("reset@test.com"), eq("654321"),
                eq(VerificationCodePurpose.EMAIL_VERIFICATION), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        Optional<Usuario> result = verificationService.verifyCode("reset@test.com", "654321");

        assertTrue(result.isEmpty());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void resendCodeNormalizaEmailAntesDeBuscarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(2);
        usuario.setEmail("socio1@demo.com");
        usuario.setNombre("Socio");

        when(usuarioRepository.findByEmailForUpdate("socio1@demo.com"))
                .thenReturn(Optional.of(usuario));
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

        verificationService.resendCode("  SOCIO1@DEMO.COM  ");

        verify(usuarioRepository).findByEmailForUpdate("socio1@demo.com");
        ArgumentCaptor<VerificationCode> captor = ArgumentCaptor.forClass(VerificationCode.class);
        verify(verificationCodeRepository).save(captor.capture());
        assertEquals(usuario, captor.getValue().getUsuario());
        verify(emailService).sendVerificationCode(eq("socio1@demo.com"), anyString(), anyString());
    }
}
