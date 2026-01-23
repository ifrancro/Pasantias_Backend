package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.producto.ProductoConDisponibilidadDTO;
import com.example.herbalife_clubes.services.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para gestionar la disponibilidad de productos en clubes específicos.
 * Maneja la relación club_productos sin afectar el estado global de los productos.
 */
@RestController
@RequestMapping("/api/clubes")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ClubProductoController {

    private final ProductoService productoService;

    /**
     * Toggle de disponibilidad de un producto en un club específico.
     * 
     * IMPORTANTE: Este endpoint solo modifica el campo 'disponible' en la tabla club_productos.
     * NO modifica el campo 'activo' global del producto (ese es para el administrador del Hub).
     * 
     * FLUJO:
     * 1. Si no existe relación en club_productos, la crea con disponible=false y luego la cambia a true
     * 2. Si existe relación, invierte el estado de 'disponible'
     * 3. El campo 'activo' del producto NO se toca (ese es global)
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
        ProductoConDisponibilidadDTO producto = productoService.toggleDisponibilidadEnClub(clubId, productoId);
        return ResponseEntity.ok(producto);
    }
}

