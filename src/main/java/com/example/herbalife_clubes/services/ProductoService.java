package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoConDisponibilidadDTO;

import java.util.List;

public interface ProductoService {
    /**
     * Crea un producto según el rol del usuario:
     * - ADMIN: Producto GLOBAL (club_creador_id = null, estado = APROBADO)
     * - ANFITRION: Producto LOCAL (club_creador_id = club del anfitrión, estado = PENDIENTE)
     * 
     * @param productoDTO DTO con los datos del producto
     * @param usuarioId ID del usuario autenticado
     * @param hubId ID del Hub (requerido)
     * @return ProductoDTO creado
     */
    ProductoDTO createProducto(ProductoDTO productoDTO, Integer usuarioId, Integer hubId);
    
    /**
     * Crea un producto directamente desde un Hub (método legacy - mantener para compatibilidad).
     * NO crea relaciones ClubProducto automáticamente.
     * El producto aparecerá en el catálogo del hub y cada club podrá habilitarlo individualmente.
     * 
     * @param productoDTO DTO con los datos del producto
     * @param hubId ID del Hub
     * @return ProductoDTO creado
     */
    ProductoDTO createProductoFromHub(ProductoDTO productoDTO, Integer hubId);
    
    /**
     * Cambia el estado de aprobación de un producto (solo ADMIN).
     * 
     * @param productoId ID del producto
     * @param estadoAprobacion Nuevo estado (APROBADO | RECHAZADO)
     * @return ProductoDTO actualizado
     */
    ProductoDTO cambiarEstadoAprobacion(Integer productoId, String estadoAprobacion);
    
    ProductoDTO updateProducto(Integer productoId, ProductoDTO productoDTO);
    ProductoDTO getProducto(Integer productoId);
    ProductoDTO getProductoPublico(Integer productoId); // Sin ingredientes y sin PENDIENTE
    List<ProductoDTO> getProductos();
    List<ProductoDTO> getProductosPublicos(); // Sin ingredientes y sin PENDIENTE
    List<ProductoDTO> getProductosByClub(Integer clubId);
    List<ProductoDTO> getProductosByClubPublico(Integer clubId); // Sin ingredientes y sin PENDIENTE
    /**
     * Productos de un club filtrados por tipo (GLOBAL o LOCAL).
     * Para admin/anfitrión: incluye ingredientes y PENDIENTE.
     */
    List<ProductoDTO> getProductosByClubAndTipo(Integer clubId, String tipo);
    /**
     * Productos de un club filtrados por tipo, versión pública (sin ingredientes, sin PENDIENTE).
     */
    List<ProductoDTO> getProductosByClubPublicoAndTipo(Integer clubId, String tipo);
    ProductoDTO activarProducto(Integer productoId);
    ProductoDTO desactivarProducto(Integer productoId);
    
    /**
     * Obtiene todos los productos de un Hub (catálogo completo).
     * Incluye información de disponibilidad en un club específico si se proporciona.
     * 
     * @param hubId ID del Hub
     * @param clubId ID del club (opcional) - si se proporciona, incluye info de disponibilidad
     * @return Lista de productos con información de disponibilidad
     */
    List<ProductoConDisponibilidadDTO> getProductosByHub(Integer hubId, Integer clubId);
    
    /**
     * Toggle de disponibilidad de un producto en un club específico.
     * Solo modifica el campo 'disponible' en la tabla club_productos.
     * NO modifica el campo 'activo' global del producto.
     * 
     * @param clubId ID del club
     * @param productoId ID del producto
     * @return DTO del producto con el nuevo estado de disponibilidad
     */
    ProductoConDisponibilidadDTO toggleDisponibilidadEnClub(Integer clubId, Integer productoId);
}

