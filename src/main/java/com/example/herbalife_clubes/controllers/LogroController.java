package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.logro.LogroDTO;
import com.example.herbalife_clubes.dtos.logro.LogroProgresoDTO;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.LogroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logros")
@CrossOrigin("*")
public class LogroController {
    @Autowired
    private LogroService logroService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Crea un logro según el rol del usuario autenticado.
     * 
     * FLUJO:
     * - ADMIN: Crea logro global (club_creador_id = null, estado = APROBADO)
     * - ANFITRION: Crea logro local (club_creador_id = club del anfitrión, estado = PENDIENTE)
     * 
     * Validaciones:
     * - fecha_inicio y fecha_fin son obligatorios
     * - fecha_inicio no puede ser posterior a fecha_fin
     * - fecha_inicio no puede ser anterior a la fecha actual
     * 
     * @param logroDTO DTO con los datos del logro (debe incluir fechaInicio y fechaFin)
     * @return LogroDTO creado
     */
    @PostMapping
    public ResponseEntity<LogroDTO> createLogro(@RequestBody LogroDTO logroDTO) {
        // Validar autenticación
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Obtener usuario autenticado
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Crear logro según el rol
        LogroDTO savedLogroDTO = logroService.createLogro(logroDTO, usuario.getId());
        return new ResponseEntity<>(savedLogroDTO, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<LogroDTO>> getLogros() {
        List<LogroDTO> logros = logroService.getLogros();
        return ResponseEntity.ok(logros);
    }

    /**
     * Progreso del socio en retos compuestos (por membresía).
     * El path variable es el id de {@code membresias} (socio en su club), no el id de usuario.
     */
    @GetMapping("/socio/{membresiaId}/progreso")
    public ResponseEntity<List<LogroProgresoDTO>> getProgresoSocio(@PathVariable Integer membresiaId) {
        return ResponseEntity.ok(logroService.getProgresoSocio(membresiaId));
    }

    @GetMapping("{id}")
    public ResponseEntity<LogroDTO> getLogro(@PathVariable Integer id) {
        LogroDTO logroDTO = logroService.getLogro(id);
        return ResponseEntity.ok(logroDTO);
    }

    @PutMapping("{id}")
    public ResponseEntity<LogroDTO> updateLogro(@PathVariable Integer id, @RequestBody LogroDTO logroDTO) {
        LogroDTO updatedLogroDTO = logroService.updateLogro(id, logroDTO);
        return ResponseEntity.ok(updatedLogroDTO);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteLogro(@PathVariable Integer id) {
        logroService.deleteLogro(id);
        return ResponseEntity.ok("Logro eliminado exitosamente");
    }

    @PatchMapping("{id}/activar")
    public ResponseEntity<LogroDTO> activarLogro(@PathVariable Integer id) {
        LogroDTO logroDTO = logroService.activarLogro(id);
        return ResponseEntity.ok(logroDTO);
    }

    @PatchMapping("{id}/inactivar")
    public ResponseEntity<LogroDTO> inactivarLogro(@PathVariable Integer id) {
        LogroDTO logroDTO = logroService.inactivarLogro(id);
        return ResponseEntity.ok(logroDTO);
    }

    /**
     * Cambia el estado de aprobación de un logro (solo ADMIN).
     * 
     * @param id ID del logro
     * @param estadoAprobacion Nuevo estado (APROBADO | RECHAZADO)
     * @return LogroDTO actualizado
     */
    @PatchMapping("{id}/estado-aprobacion")
    public ResponseEntity<LogroDTO> cambiarEstadoAprobacion(
            @PathVariable Integer id,
            @RequestParam String estadoAprobacion) {
        
        // Validar autenticación
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Obtener usuario autenticado
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Validar que el rol sea ADMIN
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!"ADMIN".equalsIgnoreCase(rolNombre)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Cambiar estado de aprobación
        LogroDTO logroDTO = logroService.cambiarEstadoAprobacion(id, estadoAprobacion);
        return ResponseEntity.ok(logroDTO);
    }
}

