package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.common.PagedResponse;
import com.example.herbalife_clubes.dtos.membresia.ArbolReferidosDTO;
import com.example.herbalife_clubes.dtos.membresia.EstadoComboDTO;
import com.example.herbalife_clubes.dtos.membresia.MembresiaDTO;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.MembresiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membresias")
public class MembresiaController {
    @Autowired
    private MembresiaService membresiaService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<MembresiaDTO> createMembresia(@RequestBody MembresiaDTO membresiaDTO,
                                                         @RequestParam Integer usuarioId,
                                                         @RequestParam Integer clubId,
                                                         @RequestParam(required = false) Integer nivelId,
                                                         @RequestParam(required = false) Integer referidoPorMembresiaId) {
        MembresiaDTO savedMembresiaDTO = membresiaService.createMembresia(membresiaDTO, usuarioId, clubId, nivelId, referidoPorMembresiaId);
        return new ResponseEntity<>(savedMembresiaDTO, HttpStatus.CREATED);
    }

    @GetMapping("{id}")
    public ResponseEntity<MembresiaDTO> getMembresia(@PathVariable Integer id) {
        MembresiaDTO membresiaDTO = membresiaService.getMembresia(id);
        return ResponseEntity.ok(membresiaDTO);
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<MembresiaDTO> getMembresiaByUsuario(@PathVariable Integer usuarioId) {
        MembresiaDTO membresiaDTO = membresiaService.getMembresiaByUsuario(usuarioId);
        return ResponseEntity.ok(membresiaDTO);
    }

    /**
     * Legacy: lista completa de membresías del club.
     * Preferir {@link #getMembresiasByClubPaginadas}.
     */
    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<MembresiaDTO>> getMembresiasByClub(@PathVariable Integer clubId) {
        List<MembresiaDTO> membresias = membresiaService.getMembresiasByClub(clubId);
        return ResponseEntity.ok(membresias);
    }

    /**
     * Membresías del club con paginación y metadata.
     * q opcional (nombre, apellido, número de socio).
     */
    @GetMapping("/club/{clubId}/paginadas")
    public ResponseEntity<PagedResponse<MembresiaDTO>> getMembresiasByClubPaginadas(
            @PathVariable Integer clubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(
                membresiaService.getMembresiasByClubPaginadas(clubId, page, size, q));
    }

    @PatchMapping("{id}/estado")
    public ResponseEntity<MembresiaDTO> cambiarEstado(@PathVariable Integer id, @RequestParam String estado) {
        MembresiaDTO membresiaDTO = membresiaService.cambiarEstado(id, estado);
        return ResponseEntity.ok(membresiaDTO);
    }

    @PatchMapping("{id}/nivel")
    public ResponseEntity<MembresiaDTO> cambiarNivel(@PathVariable Integer id, @RequestParam Integer nivelId) {
        MembresiaDTO membresiaDTO = membresiaService.cambiarNivel(id, nivelId);
        return ResponseEntity.ok(membresiaDTO);
    }

    @PatchMapping("{id}/puntos")
    public ResponseEntity<MembresiaDTO> actualizarPuntos(@PathVariable Integer id, @RequestParam Integer puntos) {
        MembresiaDTO membresiaDTO = membresiaService.actualizarPuntos(id, puntos);
        return ResponseEntity.ok(membresiaDTO);
    }

    @PatchMapping("{id}/recalcular-puntos")
    public ResponseEntity<MembresiaDTO> recalcularPuntosPorAsistencias(@PathVariable Integer id) {
        MembresiaDTO membresiaDTO = membresiaService.recalcularPuntosPorAsistencias(id);
        return ResponseEntity.ok(membresiaDTO);
    }

    /**
     * Obtiene el árbol de referidos de una membresía de forma recursiva.
     * Solo accesible para usuarios con rol ADMIN.
     * 
     * @param id ID de la membresía raíz
     * @return Árbol de referidos con estructura recursiva
     */
    /**
     * Indica si el socio consumió al menos un producto tipo COMBO en pedidos ENTREGADOS.
     */
    @GetMapping("{id}/estado-combo")
    public ResponseEntity<EstadoComboDTO> getEstadoCombo(@PathVariable Integer id) {
        return ResponseEntity.ok(membresiaService.getEstadoCombo(id));
    }

    @GetMapping("/me/arbol-referidos")
    public ResponseEntity<ArbolReferidosDTO> getMiArbolReferidos() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Obtener la membresía y luego el árbol de referidos del usuario autenticado
        MembresiaDTO membresia = membresiaService.getMembresiaByUsuario(usuario.getId());
        ArbolReferidosDTO arbol = membresiaService.getArbolReferidos(membresia.getId());
        return ResponseEntity.ok(arbol);
    }

    @GetMapping("{id}/arbol-referidos")
    public ResponseEntity<ArbolReferidosDTO> getArbolReferidos(@PathVariable Integer id) {
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

        // Validar que el rol sea ADMIN o ANFITRION
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!"ADMIN".equalsIgnoreCase(rolNombre) && !"ANFITRION".equalsIgnoreCase(rolNombre)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Obtener árbol de referidos
        ArbolReferidosDTO arbol = membresiaService.getArbolReferidos(id);
        return ResponseEntity.ok(arbol);
    }

    /**
     * Legacy: page/size aplicados pero respuesta {@code List} sin metadata.
     * Preferir {@link #buscarMiembrosPaginado}.
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<MembresiaDTO>> buscarMiembros(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(membresiaService.buscarMiembrosGlobal(q, page, size));
    }

    /**
     * Búsqueda global paginada con metadata (content, totalElements, hasNext, …).
     */
    @GetMapping("/buscar/paginado")
    public ResponseEntity<PagedResponse<MembresiaDTO>> buscarMiembrosPaginado(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(membresiaService.buscarMiembrosGlobalPaginado(q, page, size));
    }
}

