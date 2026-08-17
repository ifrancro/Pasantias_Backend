package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.combo.ComboCreateRequest;
import com.example.herbalife_clubes.dtos.combo.ComboDTO;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.ComboClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubes/{clubId}/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboClubService comboClubService;
    private final UsuarioRepository usuarioRepository;
    private final ClubRepository clubRepository;

    /**
     * Lista todos los combos de un club.
     */
    @GetMapping
    public ResponseEntity<List<ComboDTO>> getCombosByClub(@PathVariable Integer clubId) {
        return ResponseEntity.ok(comboClubService.getCombosByClub(clubId));
    }

    /**
     * Obtiene un combo por su ID.
     */
    @GetMapping("/{comboId}")
    public ResponseEntity<ComboDTO> getCombo(@PathVariable Integer clubId, @PathVariable Integer comboId) {
        return ResponseEntity.ok(comboClubService.getCombo(comboId));
    }

    /**
     * Crea un nuevo combo en el club (ANFITRION o ADMIN).
     */
    @PostMapping
    public ResponseEntity<?> createCombo(
            @PathVariable Integer clubId,
            @RequestBody ComboCreateRequest request) {

        Usuario usuario = getUsuarioAutenticado();
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!esAnfitrionDelClub(usuario, clubId) && !esAdmin(usuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            ComboDTO created = comboClubService.createCombo(clubId, request);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Actualiza un combo existente (ANFITRION o ADMIN).
     */
    @PutMapping("/{comboId}")
    public ResponseEntity<?> updateCombo(
            @PathVariable Integer clubId,
            @PathVariable Integer comboId,
            @RequestBody ComboCreateRequest request) {

        Usuario usuario = getUsuarioAutenticado();
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!esAnfitrionDelClub(usuario, clubId) && !esAdmin(usuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            return ResponseEntity.ok(comboClubService.updateCombo(comboId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Toggle activo/inactivo de un combo (ANFITRION o ADMIN).
     */
    @PatchMapping("/{comboId}/toggle")
    public ResponseEntity<ComboDTO> toggleCombo(
            @PathVariable Integer clubId,
            @PathVariable Integer comboId) {

        Usuario usuario = getUsuarioAutenticado();
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!esAnfitrionDelClub(usuario, clubId) && !esAdmin(usuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(comboClubService.toggleCombo(comboId));
    }

    /**
     * Elimina un combo (ANFITRION o ADMIN).
     */
    @DeleteMapping("/{comboId}")
    public ResponseEntity<Void> deleteCombo(
            @PathVariable Integer clubId,
            @PathVariable Integer comboId) {

        Usuario usuario = getUsuarioAutenticado();
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!esAnfitrionDelClub(usuario, clubId) && !esAdmin(usuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        comboClubService.deleteCombo(comboId);
        return ResponseEntity.noContent().build();
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
