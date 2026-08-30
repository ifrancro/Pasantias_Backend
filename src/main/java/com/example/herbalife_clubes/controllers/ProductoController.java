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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/productos")
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
     * Lista productos.
     * - Sin clubId (Admin/Anfitrión): todos los productos. Admin: DTO completo (clubCreadorNombre, ingredientes, imagenUrl).
     * - Con clubId: menú del club (productos con disponible=true en ese club). El Admin puede usar clubId para supervisar el menú de cualquier club.
     * - Con clubId y tipo (GLOBAL|LOCAL): filtrados por tipo. Cada ítem incluye tipo y estadoAprobacion.
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
                        ? productoService.getProductosByClubAndTipoParaAnfitrion(clubId, tipo)
                        : productoService.getProductosByClubPublicoAndTipo(clubId, tipo);
            } else {
                productos = esAdminOAnfitrion
                        ? productoService.getProductosByClubParaAnfitrion(clubId)
                        : productoService.getProductosByClubPublico(clubId);
            }
        } else {
            productos = esAdminOAnfitrion
                    ? productoService.getProductos()
                    : productoService.getProductosPublicos();
        }
        return ResponseEntity.ok(productos);
    }

    /**
     * Bandeja de entrada del Admin: solo productos con estado_aprobacion = PENDIENTE.
     * DTO incluye clubCreadorNombre, ingredientes, imagenUrl.
     * Solo ADMIN.
     */
    @GetMapping("/pendientes")
    public ResponseEntity<List<ProductoDTO>> getProductosPendientes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!"ADMIN".equalsIgnoreCase(rolNombre)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(productoService.getProductosPendientes());
    }

    /**
     * Historial: productos aprobados. Opcional clubId para filtrar por club creador.
     * DTO completo (clubCreadorNombre, ingredientes, imagenUrl). Solo ADMIN.
     */
    @GetMapping("/aprobados")
    public ResponseEntity<List<ProductoDTO>> getProductosAprobados(@RequestParam(required = false) Integer clubId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!"ADMIN".equalsIgnoreCase(rolNombre)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(productoService.getProductosAprobados(clubId));
    }

    /**
     * Historial: productos rechazados. Opcional clubId para filtrar por club creador.
     * DTO completo (clubCreadorNombre, ingredientes, imagenUrl). Solo ADMIN.
     */
    @GetMapping("/rechazados")
    public ResponseEntity<List<ProductoDTO>> getProductosRechazados(@RequestParam(required = false) Integer clubId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!"ADMIN".equalsIgnoreCase(rolNombre)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(productoService.getProductosRechazados(clubId));
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
        Usuario usuario = usuarioAutenticadoOrNull();
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ProductoDTO updatedProductoDTO = productoService.updateProducto(id, productoDTO, usuario.getId());
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
     * URL sin cambios. {@code comentario} opcional en APROBADO; obligatorio en RECHAZADO.
     */
    @PatchMapping("{id}/estado-aprobacion")
    public ResponseEntity<ProductoDTO> cambiarEstadoAprobacion(
            @PathVariable Integer id,
            @RequestParam String estadoAprobacion,
            @RequestParam(required = false) String comentario) {

        Usuario usuario = usuarioAutenticadoOrNull();
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!"ADMIN".equalsIgnoreCase(rolNombre)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ProductoDTO productoDTO = productoService.cambiarEstadoAprobacion(
                id, estadoAprobacion, comentario, usuario.getId());
        return ResponseEntity.ok(productoDTO);
    }

    /**
     * Reenvía un producto LOCAL RECHAZADO a PENDIENTE. Solo el anfitrión propietario.
     */
    @PatchMapping("{id}/reenviar")
    public ResponseEntity<ProductoDTO> reenviarProducto(@PathVariable Integer id) {
        Usuario usuario = usuarioAutenticadoOrNull();
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(productoService.reenviarProducto(id, usuario.getId()));
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

    /**
     * Sube una imagen para un producto. Guarda el archivo en el servidor y devuelve la URL pública.
     * El cliente debe enviar el campo "file" (multipart/form-data).
     *
     * @param file imagen (jpeg, png, webp)
     * @return JSON con "imagenUrl" para usar en el DTO del producto
     */
    @PostMapping("/subir-imagen")
    public ResponseEntity<java.util.Map<String, String>> subirImagenProducto(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            return ResponseEntity.badRequest().build();
        }
        String extension = "jpg";
        if (contentType.contains("png")) extension = "png";
        else if (contentType.contains("webp")) extension = "webp";
        else if (contentType.contains("jpeg")) extension = "jpg";

        try {
            String filename = "producto-" + UUID.randomUUID() + "." + extension;
            Path uploadDir = Paths.get("uploads", "productos");
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Ruta relativa que el cliente puede combinar con su baseUrl: /api/productos/imagenes/{filename}
            String imagenUrl = "/api/productos/imagenes/" + filename;
            return ResponseEntity.ok(java.util.Map.of("imagenUrl", imagenUrl));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Sirve una imagen de producto subida (GET /api/productos/imagenes/{filename}).
     */
    @GetMapping("/imagenes/{filename:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> servirImagenProducto(@PathVariable String filename) {
        try {
            Path path = Paths.get("uploads", "productos").resolve(filename);
            if (!Files.exists(path) || !Files.isReadable(path)) {
                return ResponseEntity.notFound().build();
            }
            org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(path.toFile());
            String contentType = Files.probeContentType(path);
            if (contentType == null) contentType = "image/jpeg";
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Usuario usuarioAutenticadoOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return usuarioRepository.findByEmail(authentication.getName()).orElse(null);
    }
}

