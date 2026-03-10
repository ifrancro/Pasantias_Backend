package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin("*")
public class ProductoController {
    @Autowired
    private ProductoService productoService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Crea un producto según el rol del usuario autenticado.
     * 
     * FLUJO:
     * - ADMIN: Crea producto GLOBAL (club_creador_id = null, estado = APROBADO)
     * - ANFITRION: Crea producto LOCAL (club_creador_id = club del anfitrión, estado = PENDIENTE)
     * 
     * @param productoDTO DTO con los datos del producto (debe incluir hubId)
     * @return ProductoDTO creado
     */
    @PostMapping
    public ResponseEntity<ProductoDTO> createProducto(@RequestBody ProductoDTO productoDTO) {
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

        // Validar que se proporcionó hubId
        if (productoDTO.getHubId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        // Crear producto según el rol
        ProductoDTO savedProductoDTO = productoService.createProducto(
                productoDTO, 
                usuario.getId(), 
                productoDTO.getHubId()
        );
        
        return new ResponseEntity<>(savedProductoDTO, HttpStatus.CREATED);
    }

    /**
     * Lista productos. Con clubId: productos del club (disponibles en el club).
     * Con clubId y tipo (GLOBAL|LOCAL): filtrados por tipo para agrupar en front.
     * Cada ítem incluye tipo y estadoAprobacion.
     */
    @GetMapping
    public ResponseEntity<List<ProductoDTO>> getProductos(
            @RequestParam(required = false) Integer clubId,
            @RequestParam(required = false) String tipo) {
        // Detectar si es usuario autenticado y su rol
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdminOAnfitrion = false;

        if (authentication != null && authentication.getName() != null) {
            Usuario usuario = usuarioRepository.findByEmail(authentication.getName()).orElse(null);
            if (usuario != null && usuario.getRol() != null) {
                String rolNombre = usuario.getRol().getNombre();
                esAdminOAnfitrion = "ADMIN".equalsIgnoreCase(rolNombre) || "ANFITRION".equalsIgnoreCase(rolNombre);
            }
        }

        List<ProductoDTO> productos;
        if (clubId != null) {
            if (tipo != null && !tipo.isBlank()) {
                productos = esAdminOAnfitrion
                        ? productoService.getProductosByClubAndTipo(clubId, tipo)
                        : productoService.getProductosByClubPublicoAndTipo(clubId, tipo);
            } else {
                productos = esAdminOAnfitrion
                        ? productoService.getProductosByClub(clubId)
                        : productoService.getProductosByClubPublico(clubId);
            }
        } else {
            productos = esAdminOAnfitrion
                    ? productoService.getProductos()
                    : productoService.getProductosPublicos();
        }
        return ResponseEntity.ok(productos);
    }

    @GetMapping("{id}")
    public ResponseEntity<ProductoDTO> getProducto(@PathVariable Integer id) {
        // Detectar si es usuario autenticado y su rol
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdminOAnfitrion = false;
        
        if (authentication != null && authentication.getName() != null) {
            Usuario usuario = usuarioRepository.findByEmail(authentication.getName()).orElse(null);
            if (usuario != null && usuario.getRol() != null) {
                String rolNombre = usuario.getRol().getNombre();
                esAdminOAnfitrion = "ADMIN".equalsIgnoreCase(rolNombre) || "ANFITRION".equalsIgnoreCase(rolNombre);
            }
        }
        
        // Si es admin/anfitrión, devolver completo (con ingredientes)
        // Si es socio o público, devolver versión pública (sin ingredientes, sin PENDIENTE)
        ProductoDTO productoDTO = esAdminOAnfitrion 
                ? productoService.getProducto(id)
                : productoService.getProductoPublico(id);
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
     * Cambia el estado de aprobación de un producto (solo ADMIN).
     * 
     * @param id ID del producto
     * @param estadoAprobacion Nuevo estado (APROBADO | RECHAZADO)
     * @return ProductoDTO actualizado
     */
    @PatchMapping("{id}/estado-aprobacion")
    public ResponseEntity<ProductoDTO> cambiarEstadoAprobacion(
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
        ProductoDTO productoDTO = productoService.cambiarEstadoAprobacion(id, estadoAprobacion);
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

