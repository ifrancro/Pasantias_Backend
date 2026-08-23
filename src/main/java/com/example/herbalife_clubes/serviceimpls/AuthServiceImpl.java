package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.auth.AuthenticationRequest;
import com.example.herbalife_clubes.dtos.auth.AuthenticationResponse;
import com.example.herbalife_clubes.dtos.auth.GoogleAuthRequest;
import com.example.herbalife_clubes.dtos.auth.RegisterRequest;
import com.example.herbalife_clubes.dtos.auth.RegisterBasicoRequest;
import com.example.herbalife_clubes.dtos.auth.RegisterBasicoResponse;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.security.JwtService;
import com.example.herbalife_clubes.services.AuthService;
import com.example.herbalife_clubes.services.VerificationService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final VerificationService verificationService;

    @Value("${google.client-id:812612197014-t4ud108qj177tpoh5in0qf6hiv1rqo4h.apps.googleusercontent.com}")
    private String googleClientId;

    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        Usuario usuario = new Usuario();

        usuario.setNombre(request.getNombre() != null && !request.getNombre().isBlank() 
                ? request.getNombre() : "Usuario");
        usuario.setApellido(request.getApellido() != null && !request.getApellido().isBlank() 
                ? request.getApellido() : "Default");
        usuario.setEmail(request.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setTelefono(request.getTelefono());
        usuario.setRedesSociales(request.getRedesSociales());
        
        if (request.getFechaNacimiento() != null && !request.getFechaNacimiento().isBlank()) {
            try {
                usuario.setFechaNacimiento(LocalDate.parse(request.getFechaNacimiento(), 
                        DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (Exception e) {
                // Si hay error en el parseo, se deja null
            }
        }

        // Asignar rol - Siempre asignar USUARIO_BASICO por defecto
        // El rolId enviado por el cliente se IGNORA por seguridad (solo ADMIN puede asignar roles)
        Rol rolDefault = rolRepository.findByNombre("USUARIO_BASICO")
                .orElseGet(() -> {
                    Rol nuevoRol = new Rol();
                    nuevoRol.setNombre("USUARIO_BASICO");
                    return rolRepository.save(nuevoRol);
                });
        usuario.setRol(rolDefault);

        // Estado inicial: pendiente de verificación por email
        usuario.setEstado("PENDIENTE_VERIFICACION");

        usuarioRepository.save(usuario);

        // El fallo de envío NO se oculta: si el OTP no salió, el usuario queda
        // atrapado sin forma de activarse y sin saber por qué. Que el registro
        // devolviera 200 con el correo caído es lo que dejó este bug invisible
        // durante meses.
        try {
            verificationService.generateAndSendCode(usuario);
        } catch (Exception e) {
            log.error("Error al enviar código de verificación para {}: {}",
                    usuario.getEmail(), e.getMessage(), e);
            throw new com.example.herbalife_clubes.exceptions.EmailDeliveryException(
                    "No pudimos enviar el código de verificación a tu correo. "
                            + "Revisa la dirección o intenta de nuevo en unos minutos.", e);
        }

        // Sin token: la sesión se emite en /verify-email, no acá.
        return AuthenticationResponse.builder()
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .rolNombre(usuario.getRol().getNombre())
                .requiresVerification(true)
                .build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Spring trata PENDIENTE_VERIFICACION como DisabledException (isEnabled=false)
        // antes de validar la contraseña. Aquí separamos ese caso de una cuenta
        // realmente deshabilitada/bloqueada, y solo tras confirmar la password.
        Usuario pendiente = usuarioRepository.findByEmail(request.getEmail()).orElse(null);
        if (pendiente != null
                && "PENDIENTE_VERIFICACION".equalsIgnoreCase(pendiente.getEstado())) {
            if (pendiente.getPasswordHash() == null
                    || !passwordEncoder.matches(request.getPassword(), pendiente.getPasswordHash())) {
                throw new org.springframework.security.authentication.BadCredentialsException(
                        "Credenciales incorrectas");
            }
            throw new com.example.herbalife_clubes.exceptions.EmailNotVerifiedException(
                    "Debes verificar tu correo para continuar.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String jwtToken = jwtService.generateToken(usuario);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .rolNombre(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                .build();
    }

    /**
     * Autentica un usuario con Google Sign-In.
     * Recibe el idToken generado por el SDK de Google en Flutter,
     * lo valida con las APIs de Google, y si es válido:
     * - Si el usuario ya existe (por email): inicia sesión (y activa si estaba pendiente).
     * - Si el usuario no existe: lo registra automáticamente.
     * En ambos casos devuelve un JWT propio del sistema.
     */
    public AuthenticationResponse authenticateWithGoogle(GoogleAuthRequest request) {
        try {
            // 1. Verificar el idToken con Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                throw new IllegalArgumentException("Token de Google inválido o expirado");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            return completeGoogleAuthentication(
                    payload.getEmail(),
                    (String) payload.get("given_name"),
                    (String) payload.get("family_name"),
                    payload.getEmailVerified()
            );
        } catch (DisabledException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GOOGLE AUTH] Error al autenticar con Google: {}", e.getMessage());
            throw new RuntimeException("Error al autenticar con Google: " + e.getMessage());
        }
    }

    /**
     * Lógica de negocio post-verificación del idToken de Google.
     * Separada para poder cubrirla en tests sin llamar a las APIs de Google.
     */
    public AuthenticationResponse completeGoogleAuthentication(
            String email,
            String nombre,
            String apellido,
            Boolean emailVerified
    ) {
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new IllegalArgumentException(
                    "El correo de la cuenta de Google no está verificado.");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Token de Google sin correo electrónico.");
        }

        if (nombre == null || nombre.isBlank()) nombre = "Usuario";
        if (apellido == null || apellido.isBlank()) apellido = "Google";

        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        if (usuario == null) {
            log.info("[GOOGLE AUTH] Nuevo usuario via Google: {}", email);

            Rol rolDefault = rolRepository.findByNombre("USUARIO_BASICO")
                    .orElseGet(() -> {
                        Rol nuevoRol = new Rol();
                        nuevoRol.setNombre("USUARIO_BASICO");
                        return rolRepository.save(nuevoRol);
                    });

            usuario = Usuario.builder()
                    .nombre(nombre)
                    .apellido(apellido)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .rol(rolDefault)
                    .estado("ACTIVO")
                    .build();

            usuario = usuarioRepository.save(usuario);
        } else {
            String estado = usuario.getEstado();
            if (estado == null || "ACTIVO".equalsIgnoreCase(estado)) {
                log.info("[GOOGLE AUTH] Usuario existente ACTIVO via Google: {}", email);
            } else if ("PENDIENTE_VERIFICACION".equalsIgnoreCase(estado)) {
                log.info("[GOOGLE AUTH] Activando usuario pendiente via Google: id={} email={}",
                        usuario.getId(), email);
                usuario.setEstado("ACTIVO");
                verificationService.invalidateCodes(usuario);
                usuario = usuarioRepository.save(usuario);
            } else {
                // BLOQUEADO, INACTIVO u otro estado administrativo: no reactivar.
                log.warn("[GOOGLE AUTH] Cuenta no usable via Google: email={} estado={}",
                        email, estado);
                throw new DisabledException("Usuario deshabilitado. Contacte al administrador.");
            }
        }

        String jwtToken = jwtService.generateToken(usuario);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .rolNombre(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                .requiresVerification(false)
                .build();
    }

    /**
     * Registra un usuario como USUARIO_BASICO y genera el QR de activación.
     * Este método es similar a register() pero específico para usuarios básicos
     * y devuelve el QR de activación en la respuesta.
     */
    @Override
    public RegisterBasicoResponse registerBasico(RegisterBasicoRequest request) {
        Usuario usuario = new Usuario();

        usuario.setNombre(request.getNombre() != null && !request.getNombre().isBlank() 
                ? request.getNombre() : "Usuario");
        usuario.setApellido(request.getApellido() != null && !request.getApellido().isBlank() 
                ? request.getApellido() : "Default");
        usuario.setEmail(request.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setTelefono(request.getTelefono());
        usuario.setRedesSociales(request.getRedesSociales());
        
        if (request.getFechaNacimiento() != null && !request.getFechaNacimiento().isBlank()) {
            try {
                usuario.setFechaNacimiento(LocalDate.parse(request.getFechaNacimiento(), 
                        DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (Exception e) {
                // Si hay error en el parseo, se deja null
            }
        }

        // Asignar rol USUARIO_BASICO
        Rol rolDefault = rolRepository.findByNombre("USUARIO_BASICO")
                .orElseGet(() -> {
                    Rol nuevoRol = new Rol();
                    nuevoRol.setNombre("USUARIO_BASICO");
                    return rolRepository.save(nuevoRol);
                });
        usuario.setRol(rolDefault);

        // Estado inicial: pendiente de verificación por email
        usuario.setEstado("PENDIENTE_VERIFICACION");

        usuario = usuarioRepository.save(usuario);

        // Enviar código de verificación por correo
        try {
            verificationService.generateAndSendCode(usuario);
        } catch (Exception e) {
            log.error("Error al enviar código de verificación para {}: {}",
                    usuario.getEmail(), e.getMessage(), e);
            throw new com.example.herbalife_clubes.exceptions.EmailDeliveryException(
                    "No pudimos enviar el código de verificación a tu correo. "
                            + "Revisa la dirección o intenta de nuevo en unos minutos.", e);
        }

        String jwtToken = jwtService.generateToken(usuario);
        
        // Generar QR de activación: "ACTIVATE:{userId}"
        String qrActivacionPayload = "ACTIVATE:" + usuario.getId();

        return RegisterBasicoResponse.builder()
                .token(jwtToken)
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .rolNombre(usuario.getRol().getNombre())
                .qrActivacionPayload(qrActivacionPayload)
                .build();
    }
}

