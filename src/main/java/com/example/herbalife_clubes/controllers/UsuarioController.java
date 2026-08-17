package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.auth.QrResponse;
import com.example.herbalife_clubes.dtos.usuario.UsuarioDTO;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.SocioActivationService;
import com.example.herbalife_clubes.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    
    private final UsuarioService usuarioService;
    private final SocioActivationService socioActivationService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/perfil/{usuarioId}")
    public ResponseEntity<UsuarioDTO> getPerfil(@PathVariable Integer usuarioId) {
        UsuarioDTO usuarioDTO = usuarioService.getPerfil(usuarioId);
        return ResponseEntity.ok(usuarioDTO);
    }

    @PutMapping("/perfil/{usuarioId}")
    public ResponseEntity<UsuarioDTO> actualizarPerfil(@PathVariable Integer usuarioId,
                                                         @RequestBody UsuarioDTO usuarioDTO) {
        UsuarioDTO updatedUsuarioDTO = usuarioService.actualizarPerfil(usuarioId, usuarioDTO);
        return ResponseEntity.ok(updatedUsuarioDTO);
    }

    @PatchMapping("{id}/desactivar")
    public ResponseEntity<UsuarioDTO> desactivarUsuario(@PathVariable Integer id) {
        UsuarioDTO usuarioDTO = usuarioService.desactivarUsuario(id);
        return ResponseEntity.ok(usuarioDTO);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        List<UsuarioDTO> usuarios = usuarioService.listarUsuarios();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("{id}")
    public ResponseEntity<UsuarioDTO> getUsuario(@PathVariable Integer id) {
        UsuarioDTO usuarioDTO = usuarioService.getUsuario(id);
        return ResponseEntity.ok(usuarioDTO);
    }

    /**
     * Obtiene el QR de activación o definitivo del usuario autenticado.
     * 
     * FLUJO:
     * - Si el usuario NO tiene membresía (es USUARIO_BASICO):
     *   -> Devuelve QR de activación: "ACTIVATE:{userId}"
     * - Si el usuario YA tiene membresía (es SOCIO):
     *   -> Devuelve QR definitivo: "SOCIO:{numeroSocio}"
     * 
     * Acceso: USUARIO_BASICO y SOCIO (el usuario puede ver su propio QR)
     */
    @GetMapping("/me/qr-activacion")
    public ResponseEntity<QrResponse> obtenerQrActivacion() {
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

        QrResponse qrResponse = socioActivationService.obtenerQrUsuario(usuario.getId());
        return ResponseEntity.ok(qrResponse);
    }
}

