package com.example.herbalife_clubes.controllers.auth;

import com.example.herbalife_clubes.dtos.auth.AuthenticationRequest;
import com.example.herbalife_clubes.dtos.auth.AuthenticationResponse;
import com.example.herbalife_clubes.dtos.auth.RegisterRequest;
import com.example.herbalife_clubes.dtos.auth.RegisterBasicoRequest;
import com.example.herbalife_clubes.dtos.auth.RegisterBasicoResponse;
import com.example.herbalife_clubes.dtos.auth.VerifyEmailRequest;
import com.example.herbalife_clubes.dtos.auth.ResendCodeRequest;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.AuthServiceImpl;
import com.example.herbalife_clubes.services.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;
    private final UsuarioRepository usuarioRepository;
    private final VerificationService verificationService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        boolean exists = usuarioRepository.existsByEmail(email);
        return ResponseEntity.ok(java.util.Map.of("exists", exists));
    }

    /**
     * Endpoint para registro de usuarios básicos.
     * Crea un usuario con rol USUARIO_BASICO y devuelve el QR de activación.
     * 
     * FLUJO:
     * 1. Usuario se registra -> se crea con rol USUARIO_BASICO
     * 2. Se genera QR de activación: "ACTIVATE:{userId}"
     * 3. El front genera la imagen QR con ese payload
     * 4. Usuario muestra su QR de activación
     * 5. Anfitrión escanea el QR y activa al usuario como socio
     */
    @PostMapping("/register-basico")
    public ResponseEntity<RegisterBasicoResponse> registerBasico(@Valid @RequestBody RegisterBasicoRequest request) {
        return ResponseEntity.ok(authService.registerBasico(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    /**
     * Verificar correo electrónico con código OTP.
     * 
     * @param request contiene email y código de 6 dígitos
     * @return resultado de la verificación
     */
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            boolean verified = verificationService.verifyCode(request.getEmail(), request.getCode());
            
            if (verified) {
                return ResponseEntity.ok(Map.of(
                    "verified", true,
                    "message", "Correo verificado exitosamente"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "verified", false,
                    "message", "Código inválido o expirado. Verifica e intenta de nuevo."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "verified", false,
                "message", "Error al verificar: " + e.getMessage()
            ));
        }
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
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error al reenviar código: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getAuthenticatedUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }

            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

            if (usuario == null) {
                return ResponseEntity.status(404).body("Usuario no encontrado");
            }

            usuario.setPasswordHash(null);
            return ResponseEntity.ok(usuario);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al obtener usuario autenticado: " + e.getMessage());
        }
    }
}


