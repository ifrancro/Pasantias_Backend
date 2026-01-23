package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoConDisponibilidadDTO;

import java.util.List;

public interface ProductoService {
    ProductoDTO createProducto(ProductoDTO productoDTO, Integer clubId);
    ProductoDTO updateProducto(Integer productoId, ProductoDTO productoDTO);
    ProductoDTO getProducto(Integer productoId);
    List<ProductoDTO> getProductos();
    List<ProductoDTO> getProductosByClub(Integer clubId);
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

