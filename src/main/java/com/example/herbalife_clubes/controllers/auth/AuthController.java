package com.example.herbalife_clubes.controllers.auth;

import com.example.herbalife_clubes.dtos.auth.AuthenticationRequest;
import com.example.herbalife_clubes.dtos.auth.AuthenticationResponse;
import com.example.herbalife_clubes.dtos.auth.GoogleAuthRequest;
import com.example.herbalife_clubes.dtos.auth.MeResponse;
import com.example.herbalife_clubes.dtos.auth.RegisterRequest;
import com.example.herbalife_clubes.dtos.auth.RegisterBasicoRequest;
import com.example.herbalife_clubes.dtos.auth.RegisterBasicoResponse;
import com.example.herbalife_clubes.dtos.auth.VerifyEmailRequest;
import com.example.herbalife_clubes.dtos.auth.VerifyEmailResponse;
import com.example.herbalife_clubes.security.JwtService;
import com.example.herbalife_clubes.dtos.auth.ResendCodeRequest;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.EmailDeliveryException;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.AuthServiceImpl;
import com.example.herbalife_clubes.services.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthServiceImpl authService;
    private final UsuarioRepository usuarioRepository;
    private final VerificationService verificationService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        String normalized = AuthServiceImpl.normalizeEmail(email);
        boolean exists = normalized != null && usuarioRepository.existsByEmail(normalized);
        return ResponseEntity.ok(java.util.Map.of("exists", exists));
    }

    /**
     * Endpoint para registro de usuarios básicos.
     * Crea un usuario con rol USUARIO_BASICO y devuelve el QR de activación.
     * 
     * FLUJO:
     * 1. Usuario se registra -> se crea con rol USUARIO_BASICO y PENDIENTE_VERIFICACION
     * 2. Se envía OTP por correo (sin JWT)
     * 3. Tras /verify-email el usuario pasa a ACTIVO y recibe JWT
     * 4. Se devuelve QR de activación: "ACTIVATE:{userId}" para el flujo de anfitriones
     */
    @PostMapping("/register-basico")
    public ResponseEntity<RegisterBasicoResponse> registerBasico(@Valid @RequestBody RegisterBasicoRequest request) {
        return ResponseEntity.ok(authService.registerBasico(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthenticationResponse> authenticateWithGoogle(@RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(authService.authenticateWithGoogle(request));
    }

    /**
     * Verificar correo electrónico con código OTP.
     * 
     * Es el punto donde se emite el JWT del flujo de registro por correo:
     * /register ya no devuelve token, así que sin pasar por acá no hay sesión.
     * 
     * @param request contiene email y código de 6 dígitos
     * @return la sesión ya autenticada, o 400 si el código no es válido
     */
    @PostMapping("/verify-email")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        Optional<Usuario> usuario = verificationService.verifyCode(request.getEmail(), request.getCode());
        
        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().body(VerifyEmailResponse.builder()
                .verified(false)
                .message("Código inválido o expirado. Verifica e intenta de nuevo.")
                .build());
        }

        Usuario verificado = usuario.get();
        return ResponseEntity.ok(VerifyEmailResponse.builder()
            .verified(true)
            .message("Correo verificado exitosamente")
            .token(jwtService.generateToken(verificado))
            .userId(verificado.getId())
            .email(verificado.getEmail())
            .nombre(verificado.getNombre())
            .apellido(verificado.getApellido())
            .rolNombre(verificado.getRol() != null ? verificado.getRol().getNombre() : null)
            .build());
    }

    /**
     * Reenviar código de verificación.
     * 
     * @param request contiene el email del usuario
     * @return confirmación del reenvío
     */
    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(@Valid @RequestBody ResendCodeRequest request) {
        try {
            verificationService.resendCode(request.getEmail());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Código reenviado exitosamente. Revisa tu correo."
            ));
        } catch (EmailDeliveryException e) {
            throw e;
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (isControlledResendRateLimitMessage(message)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", message
                ));
            }
            log.error("Error técnico inesperado al reenviar código ({})",
                    e.getClass().getSimpleName(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "No se pudo reenviar el código. Inténtalo nuevamente."
            ));
        } catch (Exception e) {
            log.error("Error inesperado al reenviar código ({})", e.getClass().getSimpleName(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "No se pudo reenviar el código. Inténtalo nuevamente."
            ));
        }
    }

    /**
     * Prefijo exacto del mensaje de rate limit en VerificationServiceImpl.resendCode().
     * Evita filtrar RuntimeException técnicas por e.getMessage().
     */
    private static boolean isControlledResendRateLimitMessage(String message) {
        return message != null && message.startsWith("Has excedido el límite de reenvíos (");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getAuthenticatedUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Usuario no autenticado"
                ));
            }

            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

            if (usuario == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Usuario no encontrado"
                ));
            }

            return ResponseEntity.ok(MeResponse.from(usuario));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error al obtener usuario autenticado"
            ));
        }
    }
}


