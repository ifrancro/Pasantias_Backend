package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.entities.PasswordResetToken;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.entities.VerificationCode;
import com.example.herbalife_clubes.entities.VerificationCodePurpose;
import com.example.herbalife_clubes.exceptions.EmailDeliveryException;
import com.example.herbalife_clubes.exceptions.ResetCodeInvalidException;
import com.example.herbalife_clubes.exceptions.ResetTokenInvalidException;
import com.example.herbalife_clubes.repositories.PasswordResetTokenRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.repositories.VerificationCodeRepository;
import com.example.herbalife_clubes.serviceimpls.PasswordResetServiceImpl;
import com.example.herbalife_clubes.util.TokenHashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private VerificationCodeRepository verificationCodeRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private EmailService emailService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    @BeforeEach
    void configureDefaults() {
        ReflectionTestUtils.setField(passwordResetService, "codeLength", 6);
        ReflectionTestUtils.setField(passwordResetService, "otpExpirationMinutes", 15);
        ReflectionTestUtils.setField(passwordResetService, "maxResetCodesPerDay", 5);
        ReflectionTestUtils.setField(passwordResetService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(passwordResetService, "resetTokenExpirationMinutes", 15);
        ReflectionTestUtils.setField(passwordResetService, "passwordEncoder", passwordEncoder);
    }

    @Test
    void forgotPasswordUsuarioActivoGeneraOtpPasswordResetYEnviaEmail() {
        Usuario usuario = activeUser(1, "activo@test.com", "Ana");

        when(usuarioRepository.findByEmail("activo@test.com")).thenReturn(Optional.of(usuario));
        when(verificationCodeRepository.countRecentCodes(
                eq(usuario), eq(VerificationCodePurpose.PASSWORD_RESET), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(verificationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        passwordResetService.requestPasswordReset("  ACTIVO@TEST.COM  ");

        verify(verificationCodeRepository).invalidateAllByUsuarioAndPurpose(
                usuario, VerificationCodePurpose.PASSWORD_RESET);
        ArgumentCaptor<VerificationCode> captor = ArgumentCaptor.forClass(VerificationCode.class);
        verify(verificationCodeRepository).save(captor.capture());
        assertEquals(VerificationCodePurpose.PASSWORD_RESET, captor.getValue().getPurpose());
        verify(emailService).sendPasswordResetCode(eq("activo@test.com"), eq("Ana"), anyString());
        verify(verificationCodeRepository, never()).invalidateAllByUsuarioAndPurpose(
                usuario, VerificationCodePurpose.EMAIL_VERIFICATION);
    }

    @Test
    void forgotPasswordEmailInexistenteNoEnviaCorreo() {
        when(usuarioRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset("missing@test.com");

        verify(emailService, never()).sendPasswordResetCode(anyString(), anyString(), anyString());
        verify(verificationCodeRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDIENTE_VERIFICACION", "BLOQUEADO", "INACTIVO"})
    void forgotPasswordEstadosNoActivosNoEnvianCorreo(String estado) {
        Usuario usuario = activeUser(2, "pending@test.com", "Bob");
        usuario.setEstado(estado);
        when(usuarioRepository.findByEmail("pending@test.com")).thenReturn(Optional.of(usuario));

        passwordResetService.requestPasswordReset("pending@test.com");

        verify(emailService, never()).sendPasswordResetCode(anyString(), anyString(), anyString());
        verify(verificationCodeRepository, never()).save(any());
    }

    @Test
    void forgotPasswordUsuarioGoogleActivoPuedeSolicitarReset() {
        Usuario googleUser = activeUser(3, "google@test.com", "Google User");
        googleUser.setPasswordHash(null);

        when(usuarioRepository.findByEmail("google@test.com")).thenReturn(Optional.of(googleUser));
        when(verificationCodeRepository.countRecentCodes(
                eq(googleUser), eq(VerificationCodePurpose.PASSWORD_RESET), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(verificationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        passwordResetService.requestPasswordReset("google@test.com");

        verify(emailService).sendPasswordResetCode(eq("google@test.com"), anyString(), anyString());
    }

    @Test
    void verifyResetCodeRechazaOtpEmailVerification() {
        when(verificationCodeRepository.findValidCode(
                eq("user@test.com"), eq("123456"),
                eq(VerificationCodePurpose.PASSWORD_RESET), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThrows(ResetCodeInvalidException.class,
                () -> passwordResetService.verifyResetCode("user@test.com", "123456"));

        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void verifyResetCodeCorrectoMarcaOtpUsadoYGeneraResetToken() {
        Usuario usuario = activeUser(4, "reset@test.com", "Carla");
        VerificationCode otp = VerificationCode.builder()
                .usuario(usuario)
                .code("654321")
                .purpose(VerificationCodePurpose.PASSWORD_RESET)
                .used(false)
                .failedAttempts(0)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(verificationCodeRepository.findValidCode(
                eq("reset@test.com"), eq("654321"),
                eq(VerificationCodePurpose.PASSWORD_RESET), any(LocalDateTime.class)))
                .thenReturn(Optional.of(otp));
        when(verificationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String rawToken = passwordResetService.verifyResetCode("reset@test.com", "654321");

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());
        assertTrue(otp.isUsed());
        verify(passwordResetTokenRepository).invalidateAllPendingByUsuario(usuario);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        assertEquals(TokenHashing.sha256Hex(rawToken), tokenCaptor.getValue().getTokenHash());
        assertFalse(tokenCaptor.getValue().isUsed());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void verifyResetCodeIncorrectoLanzaResetCodeInvalid() {
        when(verificationCodeRepository.findValidCode(
                anyString(), eq("000000"),
                eq(VerificationCodePurpose.PASSWORD_RESET), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        ResetCodeInvalidException ex = assertThrows(ResetCodeInvalidException.class,
                () -> passwordResetService.verifyResetCode("user@test.com", "000000"));

        assertEquals(ResetCodeInvalidException.MESSAGE, ex.getMessage());
    }

    @Test
    void verifyResetCodeIncrementaIntentosFallidosYBloqueaAlLimite() {
        Usuario usuario = activeUser(5, "limit@test.com", "Dan");
        VerificationCode activeOtp = VerificationCode.builder()
                .usuario(usuario)
                .code("111111")
                .purpose(VerificationCodePurpose.PASSWORD_RESET)
                .used(false)
                .failedAttempts(4)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(verificationCodeRepository.findValidCode(
                eq("limit@test.com"), eq("000000"),
                eq(VerificationCodePurpose.PASSWORD_RESET), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(verificationCodeRepository.findFirstByUsuario_EmailAndPurposeAndUsedFalseAndExpiresAtAfterAndFailedAttemptsLessThanOrderByCreatedAtDesc(
                eq("limit@test.com"),
                eq(VerificationCodePurpose.PASSWORD_RESET),
                any(LocalDateTime.class),
                eq(5)))
                .thenReturn(Optional.of(activeOtp));
        when(verificationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(ResetCodeInvalidException.class,
                () -> passwordResetService.verifyResetCode("limit@test.com", "000000"));

        assertEquals(5, activeOtp.getFailedAttempts());
        assertTrue(activeOtp.isUsed());

        when(verificationCodeRepository.findValidCode(
                eq("limit@test.com"), eq("111111"),
                eq(VerificationCodePurpose.PASSWORD_RESET), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThrows(ResetCodeInvalidException.class,
                () -> passwordResetService.verifyResetCode("limit@test.com", "111111"));
    }

    @Test
    void resetPasswordActualizaHashMarcaTokenUsadoEInvalidaPendientes() {
        String oldHash = passwordEncoder.encode("OldPass123");
        Usuario usuario = activeUser(6, "newpass@test.com", "Eva");
        usuario.setPasswordHash(oldHash);

        String rawToken = "raw-reset-token-value";
        PasswordResetToken stored = PasswordResetToken.builder()
                .usuario(usuario)
                .tokenHash(TokenHashing.sha256Hex(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenHashAndUsedFalseAndExpiresAtAfter(
                eq(TokenHashing.sha256Hex(rawToken)), any(LocalDateTime.class)))
                .thenReturn(Optional.of(stored));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        passwordResetService.resetPassword(rawToken, "NewPass123!");

        assertTrue(stored.isUsed());
        assertNotEquals(oldHash, usuario.getPasswordHash());
        assertTrue(passwordEncoder.matches("NewPass123!", usuario.getPasswordHash()));
        assertFalse(passwordEncoder.matches("OldPass123", usuario.getPasswordHash()));
        verify(passwordResetTokenRepository).invalidateAllPendingByUsuario(usuario);
        verify(verificationCodeRepository).invalidateAllByUsuarioAndPurpose(
                usuario, VerificationCodePurpose.PASSWORD_RESET);
    }

    @Test
    void resetPasswordSegundoUsoDelMismoTokenFalla() {
        String rawToken = "one-time-token";
        PasswordResetToken usedToken = PasswordResetToken.builder()
                .usuario(activeUser(7, "once@test.com", "Fin"))
                .tokenHash(TokenHashing.sha256Hex(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(true)
                .build();

        when(passwordResetTokenRepository.findByTokenHashAndUsedFalseAndExpiresAtAfter(
                eq(TokenHashing.sha256Hex(rawToken)), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThrows(ResetTokenInvalidException.class,
                () -> passwordResetService.resetPassword(rawToken, "NewPass123!"));

        verify(usuarioRepository, never()).save(any());
        assertTrue(usedToken.isUsed());
    }

    @Test
    void resetPasswordTokenExpiradoFalla() {
        String rawToken = "expired-token";
        when(passwordResetTokenRepository.findByTokenHashAndUsedFalseAndExpiresAtAfter(
                eq(TokenHashing.sha256Hex(rawToken)), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThrows(ResetTokenInvalidException.class,
                () -> passwordResetService.resetPassword(rawToken, "NewPass123!"));
    }

    @Test
    void forgotPasswordRateLimitSilenciosoNoEnviaEmail() {
        Usuario usuario = activeUser(8, "ratelimit@test.com", "Gus");
        when(usuarioRepository.findByEmail("ratelimit@test.com")).thenReturn(Optional.of(usuario));
        when(verificationCodeRepository.countRecentCodes(
                eq(usuario), eq(VerificationCodePurpose.PASSWORD_RESET), any(LocalDateTime.class)))
                .thenReturn(5L);

        assertDoesNotThrow(() -> passwordResetService.requestPasswordReset("ratelimit@test.com"));

        verify(emailService, never()).sendPasswordResetCode(anyString(), anyString(), anyString());
        verify(verificationCodeRepository, never()).save(any());
    }

    @Test
    void forgotPasswordEmailDeliveryExceptionNoPropagaEInvalidaOtp() {
        Usuario usuario = activeUser(9, "mailfail@test.com", "Helen");
        when(usuarioRepository.findByEmail("mailfail@test.com")).thenReturn(Optional.of(usuario));
        when(verificationCodeRepository.countRecentCodes(
                eq(usuario), eq(VerificationCodePurpose.PASSWORD_RESET), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(verificationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new EmailDeliveryException())
                .when(emailService).sendPasswordResetCode(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> passwordResetService.requestPasswordReset("mailfail@test.com"));

        verify(verificationCodeRepository, times(2)).invalidateAllByUsuarioAndPurpose(
                usuario, VerificationCodePurpose.PASSWORD_RESET);
    }

    @Test
    void forgotPasswordTrasFalloEnvioNoQuedaOtpResetValido() {
        Usuario usuario = activeUser(10, "orphan-otp@test.com", "Ivan");
        when(usuarioRepository.findByEmail("orphan-otp@test.com")).thenReturn(Optional.of(usuario));
        when(verificationCodeRepository.countRecentCodes(
                eq(usuario), eq(VerificationCodePurpose.PASSWORD_RESET), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(verificationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new EmailDeliveryException())
                .when(emailService).sendPasswordResetCode(anyString(), anyString(), anyString());

        passwordResetService.requestPasswordReset("orphan-otp@test.com");

        ArgumentCaptor<VerificationCode> savedCaptor = ArgumentCaptor.forClass(VerificationCode.class);
        verify(verificationCodeRepository).save(savedCaptor.capture());
        String generatedCode = savedCaptor.getValue().getCode();

        when(verificationCodeRepository.findValidCode(
                eq("orphan-otp@test.com"), eq(generatedCode),
                eq(VerificationCodePurpose.PASSWORD_RESET), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThrows(ResetCodeInvalidException.class,
                () -> passwordResetService.verifyResetCode("orphan-otp@test.com", generatedCode));
    }

    private static Usuario activeUser(int id, String email, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setNombre(nombre);
        usuario.setEstado("ACTIVO");
        usuario.setPasswordHash("existing-hash");
        return usuario;
    }
}
