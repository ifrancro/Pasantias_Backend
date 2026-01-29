package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin("*")
public class ProductoController {
    @Autowired
    private ProductoService productoService;

    /**
     * Crea un producto.
     * 
     * FLUJO:
     * 1. Si se proporciona hubId en el DTO: crea producto desde el hub (sin crear relaciones ClubProducto)
     *    - El producto aparecerá disponible para todos los clubs del hub
     *    - Cada club podrá habilitarlo individualmente usando el toggle
     * 
     * 2. Si se proporciona clubId como query param: mantiene comportamiento legacy
     *    - Crea producto y relación ClubProducto automáticamente
     * 
     * @param productoDTO DTO con los datos del producto (puede incluir hubId)
     * @param clubId ID del club (opcional, para compatibilidad con código legacy)
     * @return ProductoDTO creado
     */
    @PostMapping
    public ResponseEntity<ProductoDTO> createProducto(@RequestBody ProductoDTO productoDTO,
                                                       @RequestParam(required = false) Integer clubId) {
        ProductoDTO savedProductoDTO;
        
        // Si viene hubId en el DTO, crear desde hub (nuevo flujo)
        if (productoDTO.getHubId() != null) {
            savedProductoDTO = productoService.createProductoFromHub(productoDTO, productoDTO.getHubId());
        } 
        // Si viene clubId como query param, mantener comportamiento legacy
        else if (clubId != null) {
            savedProductoDTO = productoService.createProducto(productoDTO, clubId);
        } 
        // Si no viene ninguno, error
        else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .build();
        }
        
        return new ResponseEntity<>(savedProductoDTO, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProductoDTO>> getProductos(@RequestParam(required = false) Integer clubId) {
        List<ProductoDTO> productos;
        if (clubId != null) {
            productos = productoService.getProductosByClub(clubId);
        } else {
            productos = productoService.getProductos();
        }
        return ResponseEntity.ok(productos);
    }

    @GetMapping("{id}")
    public ResponseEntity<ProductoDTO> getProducto(@PathVariable Integer id) {
        ProductoDTO productoDTO = productoService.getProducto(id);
        return ResponseEntity.ok(productoDTO);
    }

    @PutMapping("{id}")
    public ResponseEntity<ProductoDTO> updateProducto(@PathVariable Integer id, @RequestBody ProductoDTO productoDTO) {
        ProductoDTO updatedProductoDTO = productoService.updateProducto(id, productoDTO);
        return ResponseEntity.ok(updatedProductoDTO);
    }

    @PatchMapping("{id}/activar")
    public ResponseEntity<ProductoDTO> activarProducto(@PathVariable Integer id) {
        ProductoDTO productoDTO = productoService.activarProducto(id);
        return ResponseEntity.ok(productoDTO);
    }

    @PatchMapping("{id}/desactivar")
    public ResponseEntity<ProductoDTO> desactivarProducto(@PathVariable Integer id) {
        ProductoDTO productoDTO = productoService.desactivarProducto(id);
        return ResponseEntity.ok(productoDTO);
    }

    /**
     * Obtiene el catálogo completo de productos de un Hub.
     * Incluye información de disponibilidad en un club específico si se proporciona.
     * 
     * Endpoint: GET /api/productos/hub/{hubId}?clubId={clubId}
     * 
     * @param hubId ID del Hub (requerido)
     * @param clubId ID del club (opcional) - si se proporciona, incluye info de disponibilidad
     * @return Lista de productos con información de disponibilidad
     */
    /**
     * Obtiene el catálogo completo de productos de un Hub.
     * Incluye información de disponibilidad en un club específico si se proporciona.
     * 
     * Endpoint: GET /api/productos/hub/{hubId}?clubId={clubId}
     * 
     * @param hubId ID del Hub (requerido)
     * @param clubId ID del club (opcional) - si se proporciona, incluye info de disponibilidad
     * @return Lista de productos con información de disponibilidad
     */
    @GetMapping("/hub/{hubId}")
    public ResponseEntity<List<com.example.herbalife_clubes.dtos.producto.ProductoConDisponibilidadDTO>> getProductosByHub(
            @PathVariable Integer hubId,
            @RequestParam(required = false) Integer clubId) {
        List<com.example.herbalife_clubes.dtos.producto.ProductoConDisponibilidadDTO> productos = 
                productoService.getProductosByHub(hubId, clubId);
        return ResponseEntity.ok(productos);
    }
}

