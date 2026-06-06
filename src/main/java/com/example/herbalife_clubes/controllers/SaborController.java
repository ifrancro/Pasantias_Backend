package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.sabor.SaborDTO;
import com.example.herbalife_clubes.dtos.sabor.SaborDisponibilidadDTO;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.SaborService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
@RequiredArgsConstructor
public class SaborController {

    private final SaborService saborService;
    private final UsuarioRepository usuarioRepository;
    private final ClubRepository clubRepository;

    // ===================== Catálogo de sabores (Admin) =====================

    /**
     * Lista todos los sabores de un Hub.
     */
    @GetMapping("/sabores")
    public ResponseEntity<List<SaborDTO>> getSabores(@RequestParam Integer hubId) {
        return ResponseEntity.ok(saborService.getSaboresByHub(hubId));
    }

    /**
     * Crea un nuevo sabor en un Hub (solo ADMIN).
     */
    @PostMapping("/sabores")
    public ResponseEntity<SaborDTO> createSabor(@RequestBody SaborDTO saborDTO) {
        Usuario usuario = getUsuarioAutenticado();
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!esAdmin(usuario)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        SaborDTO created = saborService.createSabor(saborDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Actualiza un sabor (solo ADMIN).
     */
    @PutMapping("/sabores/{id}")
    public ResponseEntity<SaborDTO> updateSabor(@PathVariable Integer id, @RequestBody SaborDTO saborDTO) {
        Usuario usuario = getUsuarioAutenticado();
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!esAdmin(usuario)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        return ResponseEntity.ok(saborService.updateSabor(id, saborDTO));
    }

    // ===================== Sabores asignados a un producto (Admin) =====================

    /**
     * Lista los sabores asignados a un producto.
     */
    @GetMapping("/productos/{productoId}/sabores")
    public ResponseEntity<List<SaborDTO>> getSaboresDeProducto(@PathVariable Integer productoId) {
        return ResponseEntity.ok(saborService.getSaboresDeProducto(productoId));
    }

    /**
     * Asigna un sabor a un producto (solo ADMIN).
     */
    @PostMapping("/productos/{productoId}/sabores/{saborId}")
    public ResponseEntity<Void> asignarSabor(@PathVariable Integer productoId, @PathVariable Integer saborId) {
        Usuario usuario = getUsuarioAutenticado();
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!esAdmin(usuario)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        saborService.asignarSaborAProducto(productoId, saborId);
        return ResponseEntity.ok().build();
    }

    /**
     * Quita un sabor de un producto (solo ADMIN).
     */
    @DeleteMapping("/productos/{productoId}/sabores/{saborId}")
    public ResponseEntity<Void> quitarSabor(@PathVariable Integer productoId, @PathVariable Integer saborId) {
        Usuario usuario = getUsuarioAutenticado();
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!esAdmin(usuario)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        saborService.quitarSaborDeProducto(productoId, saborId);
        return ResponseEntity.noContent().build();
    }

    // ===================== Disponibilidad por club (Anfitrión) =====================

    /**
     * Lista los sabores de un producto con su disponibilidad en un club específico.
     * Usado por el anfitrión en "Mi Menú" para gestionar sabores.
     */
    @GetMapping("/clubes/{clubId}/productos/{productoId}/sabores")
    public ResponseEntity<List<SaborDisponibilidadDTO>> getSaboresEnClub(
            @PathVariable Integer clubId, @PathVariable Integer productoId) {
        return ResponseEntity.ok(saborService.getSaboresDeProductoEnClub(clubId, productoId));
    }

    /**
     * Toggle de disponibilidad de un sabor en un club para un producto.
     * Usado por el anfitrión.
     */
    @PatchMapping("/clubes/{clubId}/productos/{productoId}/sabores/{saborId}/toggle")
    public ResponseEntity<SaborDisponibilidadDTO> toggleSaborEnClub(
            @PathVariable Integer clubId,
            @PathVariable Integer productoId,
            @PathVariable Integer saborId) {

        Usuario usuario = getUsuarioAutenticado();
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // Validar que es anfitrión del club
        if (!esAnfitrionDelClub(usuario, clubId) && !esAdmin(usuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(saborService.toggleSaborEnClub(clubId, productoId, saborId));
    }

    // ===================== Helpers =====================

    private Usuario getUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return usuarioRepository.findByEmail(auth.getName()).orElse(null);
    }

    private boolean esAdmin(Usuario usuario) {
        String rol = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        return "ADMIN".equalsIgnoreCase(rol);
    }

    private boolean esAnfitrionDelClub(Usuario usuario, Integer clubId) {
        return clubRepository.findById(clubId)
                .map(club -> club.getAnfitrion() != null && club.getAnfitrion().getId().equals(usuario.getId()))
                .orElse(false);
    }
}
