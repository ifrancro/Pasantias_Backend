package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.entities.VerificationCode;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.repositories.VerificationCodeRepository;
import com.example.herbalife_clubes.services.EmailService;
import com.example.herbalife_clubes.services.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.verification.code-length:6}")
    private int codeLength;

    @Value("${app.verification.expiration-minutes:15}")
    private int expirationMinutes;

    @Value("${app.verification.max-resends:5}")
    private int maxResends;

    @Override
    @Transactional
    public void generateAndSendCode(Usuario usuario) {
        // Invalidar códigos anteriores
        verificationCodeRepository.invalidateAllByUsuario(usuario);

        // Generar código numérico de N dígitos
        String code = generateNumericCode();

        // Crear y guardar el código de verificación
        VerificationCode verificationCode = VerificationCode.builder()
                .usuario(usuario)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                .used(false)
                .build();

        verificationCodeRepository.save(verificationCode);

        // Enviar por correo
        String nombre = usuario.getNombre() != null ? usuario.getNombre() : "Usuario";
        emailService.sendVerificationCode(usuario.getEmail(), nombre, code);

        log.info("Código de verificación generado y enviado para usuario: {}", usuario.getEmail());
    }

    @Override
    @Transactional
    public Optional<Usuario> verifyCode(String email, String code) {
        var verificationCode = verificationCodeRepository
                .findValidCode(email, code, LocalDateTime.now())
                .orElse(null);

        if (verificationCode == null) {
            log.warn("Código de verificación inválido o expirado para: {}", email);
            return Optional.empty();
        }

        // Marcar código como usado
        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        // Activar el usuario
        Usuario usuario = verificationCode.getUsuario();
        usuario.setEstado("ACTIVO");
        usuario = usuarioRepository.save(usuario);

        log.info("Correo verificado exitosamente para: {}", email);
        return Optional.of(usuario);
    }

    @Override
    @Transactional
    public void resendCode(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));

        // Verificar límite de reenvíos (en las últimas 24 horas)
        long recentCount = verificationCodeRepository.countRecentCodes(
                usuario, LocalDateTime.now().minusHours(24));

        if (recentCount >= maxResends) {
            throw new RuntimeException(
                    "Has excedido el límite de reenvíos (" + maxResends + "). Intenta de nuevo en 24 horas.");
        }

        // Generar y enviar nuevo código
        generateAndSendCode(usuario);
    }

    @Override
    @Transactional
    public void invalidateCodes(Usuario usuario) {
        verificationCodeRepository.invalidateAllByUsuario(usuario);
        log.info("Códigos OTP invalidados para usuario: {}", usuario.getEmail());
    }

    /**
     * Genera un código numérico seguro de la longitud configurada.
     */
    private String generateNumericCode() {
        int max = (int) Math.pow(10, codeLength);
        int code = secureRandom.nextInt(max);
        return String.format("%0" + codeLength + "d", code);
    }
}
