package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.entities.VerificationCode;
import com.example.herbalife_clubes.entities.VerificationCodePurpose;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.repositories.VerificationCodeRepository;
import com.example.herbalife_clubes.services.EmailService;
import com.example.herbalife_clubes.services.VerificationService;
import com.example.herbalife_clubes.util.SecureOtpGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    @Value("${app.verification.code-length:6}")
    private int codeLength;

    @Value("${app.verification.expiration-minutes:15}")
    private int expirationMinutes;

    @Value("${app.verification.max-resends:5}")
    private int maxResends;

    @Override
    @Transactional
    public void generateAndSendCode(Usuario usuario) {
        generateAndSendCode(usuario, VerificationCodePurpose.EMAIL_VERIFICATION);
    }

    @Override
    @Transactional
    public Optional<Usuario> verifyCode(String email, String code) {
        String normalizedEmail = AuthServiceImpl.normalizeEmail(email);
        var verificationCode = verificationCodeRepository
                .findValidCode(
                        normalizedEmail,
                        code,
                        VerificationCodePurpose.EMAIL_VERIFICATION,
                        LocalDateTime.now())
                .orElse(null);

        if (verificationCode == null) {
            log.warn("Código de verificación inválido o expirado para: {}", normalizedEmail);
            return Optional.empty();
        }

        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        Usuario usuario = verificationCode.getUsuario();
        usuario.setEstado("ACTIVO");
        usuario = usuarioRepository.save(usuario);

        log.info("Correo verificado exitosamente para: {}", normalizedEmail);
        return Optional.of(usuario);
    }

    @Override
    @Transactional
    public void resendCode(String email) {
        String normalizedEmail = AuthServiceImpl.normalizeEmail(email);
        Usuario usuario = usuarioRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con email: " + normalizedEmail));

        long recentCount = verificationCodeRepository.countRecentCodes(
                usuario,
                VerificationCodePurpose.EMAIL_VERIFICATION,
                LocalDateTime.now().minusHours(24));

        if (recentCount >= maxResends) {
            throw new RuntimeException(
                    "Has excedido el límite de reenvíos (" + maxResends + "). Intenta de nuevo en 24 horas.");
        }

        generateAndSendCode(usuario, VerificationCodePurpose.EMAIL_VERIFICATION);
    }

    @Override
    @Transactional
    public void invalidateCodes(Usuario usuario) {
        verificationCodeRepository.invalidateAllByUsuarioAndPurpose(
                usuario, VerificationCodePurpose.EMAIL_VERIFICATION);
        log.info("Códigos OTP EMAIL_VERIFICATION invalidados para usuario: {}", usuario.getEmail());
    }

    private void generateAndSendCode(Usuario usuario, VerificationCodePurpose purpose) {
        verificationCodeRepository.invalidateAllByUsuarioAndPurpose(usuario, purpose);

        String code = SecureOtpGenerator.generateNumericCode(codeLength);

        VerificationCode verificationCode = VerificationCode.builder()
                .usuario(usuario)
                .code(code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                .used(false)
                .failedAttempts(0)
                .build();

        verificationCodeRepository.save(verificationCode);

        String nombre = usuario.getNombre() != null ? usuario.getNombre() : "Usuario";
        if (purpose == VerificationCodePurpose.EMAIL_VERIFICATION) {
            emailService.sendVerificationCode(usuario.getEmail(), nombre, code);
            log.info("Código EMAIL_VERIFICATION generado y enviado para usuario: {}", usuario.getEmail());
        }
    }
}
