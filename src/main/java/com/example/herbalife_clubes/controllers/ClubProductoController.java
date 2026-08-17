package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.producto.ProductoConDisponibilidadDTO;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para gestionar la disponibilidad de productos en clubes específicos.
 * Maneja la relación club_productos sin afectar el estado global de los productos.
 */
@RestController
@RequestMapping("/api/clubes")
@RequiredArgsConstructor
public class ClubProductoController {

    private final ProductoService productoService;
    private final ClubRepository clubRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Toggle de disponibilidad de un producto en un club específico.
     * 
     * IMPORTANTE: Este endpoint solo modifica el campo 'disponible' en la tabla club_productos.
     * NO modifica el campo 'activo' global del producto (ese es para el administrador del Hub).
     * 
     * FLUJO:
     * 1. Validar que el usuario autenticado es el anfitrión del club (403 si no)
     * 2. Si no existe relación en club_productos, la crea con disponible=false y luego la cambia a true
     * 3. Si existe relación, invierte el estado de 'disponible'
     * 4. El campo 'activo' del producto NO se toca (ese es global)
     * 
     * Endpoint: PATCH /api/clubes/{clubId}/productos/{productoId}/toggle
     * 
     * @param clubId ID del club
     * @param productoId ID del producto
     * @return Producto con el nuevo estado de disponibilidad
     */
    @PatchMapping("/{clubId}/productos/{productoId}/toggle")
    public ResponseEntity<ProductoConDisponibilidadDTO> toggleDisponibilidadProducto(
            @PathVariable Integer clubId,
            @PathVariable Integer productoId) {
        
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

        // Validar que el usuario es el anfitrión del club
        boolean esAnfitrion = clubRepository.findByIdAndAnfitrionId(clubId, usuario.getId()).isPresent();
        
        if (!esAnfitrion) {
            // Intentar validación alternativa por si el método del repositorio no funciona
            var clubOpt = clubRepository.findById(clubId);
            if (clubOpt.isPresent()) {
                var club = clubOpt.get();
                if (club.getAnfitrion() != null && club.getAnfitrion().getId().equals(usuario.getId())) {
                    esAnfitrion = true;
                }
            }
        }

        if (!esAnfitrion) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Si es anfitrión, proceder con el toggle
        ProductoConDisponibilidadDTO producto = productoService.toggleDisponibilidadEnClub(clubId, productoId);
        return ResponseEntity.ok(producto);
    }
}

