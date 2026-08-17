package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.auth.ActivarSocioRequest;
import com.example.herbalife_clubes.dtos.auth.ActivarSocioResponse;
import com.example.herbalife_clubes.dtos.auth.QrResponse;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.SocioActivationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para gestionar socios y su activación mediante QR.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin("*")
@RequiredArgsConstructor
public class SocioController {

    private final SocioActivationService socioActivationService;
    private final UsuarioRepository usuarioRepository;

    /**
     * Endpoint para activar un socio desde el QR escaneado.
     * Usado por: ANFITRION
     * 
     * FLUJO:
     * 1. Anfitrión escanea el QR de un usuario básico
     * 2. Se abre formulario para completar datos (referido_por, como_conocio)
     * 3. Anfitrión confirma -> se llama a este endpoint
     * 4. Backend valida que el anfitrión es dueño del club
     * 5. Backend crea membresía y actualiza rol a SOCIO
     * 6. Devuelve datos para generar QR definitivo del socio
     * 
     * Validaciones:
     * - El anfitrión debe ser dueño del club (403 si no)
     * - El payload QR debe ser válido (400 si no)
     * - El usuario debe existir (404 si no)
     * - El usuario debe ser USUARIO_BASICO (409 si ya es socio)
     * - No debe tener membresía previa (409 si existe)
     */
    @PostMapping("/clubes/{clubId}/socios/activar")
    public ResponseEntity<ActivarSocioResponse> activarSocio(
            @PathVariable Integer clubId,
            @RequestBody ActivarSocioRequest request) {
        
        // Obtener usuario autenticado (anfitrión)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            System.out.println("[DEBUG] Error: No hay autenticación");
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        System.out.println("[DEBUG] Email autenticado: " + email);
        
        Usuario anfitrion = usuarioRepository.findByEmail(email)
                .orElse(null);

        if (anfitrion == null) {
            System.out.println("[DEBUG] Error: Usuario no encontrado con email: " + email);
            return ResponseEntity.status(404).build();
        }

        System.out.println("[DEBUG] Usuario encontrado - ID: " + anfitrion.getId() + ", Email: " + anfitrion.getEmail());
        
        // Forzar carga del rol (aunque debería estar EAGER, por si acaso)
        if (anfitrion.getRol() != null) {
            String rolNombre = anfitrion.getRol().getNombre();
            System.out.println("[DEBUG] ROL DETECTADO: " + rolNombre);
            System.out.println("[DEBUG] Rol ID: " + anfitrion.getRol().getId());
        } else {
            System.out.println("[DEBUG] ERROR: Rol es NULL para usuario ID: " + anfitrion.getId());
        }

        // Validar que el rol sea ANFITRION (opcional, pero recomendado)
        // TEMPORALMENTE COMENTADO PARA DEBUG - Descomentar después
        /*
        String rolNombre = anfitrion.getRol() != null ? anfitrion.getRol().getNombre() : "";
        if (!"ANFITRION".equalsIgnoreCase(rolNombre) && !"ADMIN".equalsIgnoreCase(rolNombre)) {
            System.out.println("[DEBUG] Error 403: Rol no permitido. Rol actual: " + rolNombre);
            return ResponseEntity.status(403).build();
        }
        */

        System.out.println("[DEBUG] Llamando a activarSocio - clubId: " + clubId + ", anfitrionId: " + anfitrion.getId());
        
        // Activar socio
        ActivarSocioResponse response = socioActivationService.activarSocio(
                clubId,
                anfitrion.getId(),
                request.getActivationPayload(),
                request.getReferidoPor(),
                request.getComoConocio(),
                request.getEsClientePreferenteODistribuidor()
        );

        System.out.println("[DEBUG] Activación exitosa - Membresía ID: " + response.getMembresiaId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtiene el QR definitivo del socio autenticado.
     * 
     * FLUJO:
     * - Solo disponible para usuarios con rol SOCIO
     * - Devuelve QR definitivo: "SOCIO:{numeroSocio}"
     * 
     * Acceso: SOLO SOCIO
     */
    @GetMapping("/socios/me/qr")
    public ResponseEntity<QrResponse> obtenerQrSocio() {
        // Obtener usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(404).build();
        }

        // Validar que el rol sea SOCIO
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!"SOCIO".equalsIgnoreCase(rolNombre)) {
            return ResponseEntity.status(403).build();
        }

        QrResponse qrResponse = socioActivationService.obtenerQrSocio(usuario.getId());
        return ResponseEntity.ok(qrResponse);
    }
}

