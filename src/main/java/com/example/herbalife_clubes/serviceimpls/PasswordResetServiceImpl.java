package com.example.herbalife_clubes.serviceimpls;

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
import com.example.herbalife_clubes.services.EmailService;
import com.example.herbalife_clubes.services.PasswordResetService;
import com.example.herbalife_clubes.util.SecureOtpGenerator;
import com.example.herbalife_clubes.util.TokenHashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UsuarioRepository usuarioRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.verification.code-length:6}")
    private int codeLength;

    @Value("${app.verification.expiration-minutes:15}")
    private int otpExpirationMinutes;

    @Value("${app.verification.max-resends:5}")
    private int maxResetCodesPerDay;

    @Value("${app.password-reset.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.password-reset.token-expiration-minutes:15}")
    private int resetTokenExpirationMinutes;

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        String normalizedEmail = AuthServiceImpl.normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return;
        }

        Usuario usuario = usuarioRepository.findByEmail(normalizedEmail).orElse(null);
        if (usuario == null || !isActiveUser(usuario)) {
            return;
        }

        long recentCount = verificationCodeRepository.countRecentCodes(
                usuario,
                VerificationCodePurpose.PASSWORD_RESET,
                LocalDateTime.now().minusHours(24));
        if (recentCount >= maxResetCodesPerDay) {
            log.info("Límite diario de códigos PASSWORD_RESET alcanzado (sin respuesta pública)");
            return;
        }

        generateAndSendPasswordResetCode(usuario);
    }

    @Override
    @Transactional
    public String verifyResetCode(String email, String code) {
        String normalizedEmail = AuthServiceImpl.normalizeEmail(email);
        LocalDateTime now = LocalDateTime.now();

        Optional<VerificationCode> match = verificationCodeRepository
                .findValidCode(
                        normalizedEmail,
                        code,
                        VerificationCodePurpose.PASSWORD_RESET,
                        now)
                .filter(vc -> vc.getFailedAttempts() < maxFailedAttempts);

        if (match.isEmpty()) {
            registerFailedResetAttempt(normalizedEmail, now);
            throw new ResetCodeInvalidException();
        }

        VerificationCode verificationCode = match.get();
        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        Usuario usuario = verificationCode.getUsuario();
        if (!isActiveUser(usuario)) {
            throw new ResetCodeInvalidException();
        }

        passwordResetTokenRepository.invalidateAllPendingByUsuario(usuario);

        String rawToken = SecureOtpGenerator.generateUrlSafeToken(32);
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .usuario(usuario)
                .tokenHash(TokenHashing.sha256Hex(rawToken))
                .expiresAt(now.plusMinutes(resetTokenExpirationMinutes))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        log.info("Código PASSWORD_RESET verificado para usuario id={}", usuario.getId());
        return rawToken;
    }

    @Override
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        if (resetToken == null || resetToken.isBlank()) {
            throw new ResetTokenInvalidException();
        }

        String tokenHash = TokenHashing.sha256Hex(resetToken.trim());
        LocalDateTime now = LocalDateTime.now();

        PasswordResetToken stored = passwordResetTokenRepository
                .findByTokenHashAndUsedFalseAndExpiresAtAfter(tokenHash, now)
                .orElseThrow(ResetTokenInvalidException::new);

        Usuario usuario = stored.getUsuario();
        if (!isActiveUser(usuario)) {
            throw new ResetTokenInvalidException();
        }

        usuario.setPasswordHash(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);

        stored.setUsed(true);
        passwordResetTokenRepository.save(stored);
        passwordResetTokenRepository.invalidateAllPendingByUsuario(usuario);
        verificationCodeRepository.invalidateAllByUsuarioAndPurpose(
                usuario, VerificationCodePurpose.PASSWORD_RESET);

        log.info("Contraseña restablecida para usuario id={}", usuario.getId());
    }

    private void generateAndSendPasswordResetCode(Usuario usuario) {
        verificationCodeRepository.invalidateAllByUsuarioAndPurpose(
                usuario, VerificationCodePurpose.PASSWORD_RESET);

        String code = SecureOtpGenerator.generateNumericCode(codeLength);
        VerificationCode verificationCode = VerificationCode.builder()
                .usuario(usuario)
                .code(code)
                .purpose(VerificationCodePurpose.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .used(false)
                .failedAttempts(0)
                .build();
        verificationCodeRepository.save(verificationCode);

        String nombre = usuario.getNombre() != null ? usuario.getNombre() : "Usuario";
        try {
            emailService.sendPasswordResetCode(usuario.getEmail(), nombre, code);
            log.info("Código PASSWORD_RESET generado para usuario id={}", usuario.getId());
        } catch (EmailDeliveryException e) {
            log.error("Fallo de entrega de correo PASSWORD_RESET para usuario id={}", usuario.getId(), e);
            verificationCodeRepository.invalidateAllByUsuarioAndPurpose(
                    usuario, VerificationCodePurpose.PASSWORD_RESET);
        }
    }

    private void registerFailedResetAttempt(String normalizedEmail, LocalDateTime now) {
        verificationCodeRepository
                .findFirstByUsuario_EmailAndPurposeAndUsedFalseAndExpiresAtAfterAndFailedAttemptsLessThanOrderByCreatedAtDesc(
                        normalizedEmail,
                        VerificationCodePurpose.PASSWORD_RESET,
                        now,
                        maxFailedAttempts)
                .ifPresent(activeCode -> {
                    int attempts = activeCode.getFailedAttempts() + 1;
                    activeCode.setFailedAttempts(attempts);
                    if (attempts >= maxFailedAttempts) {
                        activeCode.setUsed(true);
                    }
                    verificationCodeRepository.save(activeCode);
                });
    }

    private static boolean isActiveUser(Usuario usuario) {
        return usuario.getEstado() != null
                && "ACTIVO".equalsIgnoreCase(usuario.getEstado());
    }
}
