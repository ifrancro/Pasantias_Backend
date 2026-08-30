package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.Usuario;

public class ProductoMapper {
    /**
     * Mapea Producto a ProductoDTO incluyendo todos los campos (para uso interno/admin)
     */
    public static ProductoDTO mapProductoToProductoDTO(Producto producto) {
        return mapProductoToProductoDTO(producto, true);
    }

    /**
     * Mapea Producto a ProductoDTO con opción de incluir ingredientes
     * @param producto Producto a mapear
     * @param incluirIngredientes Si es false, no incluye ingredientes (para endpoints públicos)
     */
    public static ProductoDTO mapProductoToProductoDTO(Producto producto, boolean incluirIngredientes) {
        ProductoDTO dto = new ProductoDTO();
        dto.setId(producto.getId());
        dto.setHubId(producto.getHub() != null ? producto.getHub().getId() : null);
        dto.setHubNombre(producto.getHub() != null ? producto.getHub().getNombre() : null);
        dto.setClubCreadorId(producto.getClubCreador() != null ? producto.getClubCreador().getId() : null);
        dto.setClubCreadorNombre(producto.getClubCreador() != null ? producto.getClubCreador().getNombreClub() : null);
        dto.setNombre(producto.getNombre());
        dto.setDescripcion(producto.getDescripcion());
        dto.setImagenUrl(producto.getImagenUrl());
        if (incluirIngredientes) {
            dto.setIngredientes(producto.getIngredientes());
        }
        dto.setPrecio(producto.getPrecio());
        dto.setPuntosValor(producto.getPuntosValor());
        dto.setTipo(producto.getTipo());
        dto.setEsCombo(Boolean.TRUE.equals(producto.getEsCombo()));
        dto.setEstadoAprobacion(producto.getEstadoAprobacion());
        dto.setActivo(producto.getActivo());
        dto.setCreatedAt(producto.getCreatedAt());
        if (incluirIngredientes) {
            dto.setComentarioRevision(producto.getComentarioRevision());
            if (producto.getRevisadoPor() != null) {
                dto.setRevisadoPorUsuarioId(producto.getRevisadoPor().getId());
                dto.setRevisadoPorNombre(nombreCompleto(producto.getRevisadoPor()));
            }
            dto.setRevisadoAt(producto.getRevisadoAt());
        }
        return dto;
    }

    private static String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre().trim() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido().trim() : "";
        String completo = (nombre + " " + apellido).trim();
        return completo.isEmpty() ? null : completo;
    }

    public static Producto mapProductoDTOToProducto(ProductoDTO dto) {
        Producto producto = new Producto();
        producto.setId(dto.getId());
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setImagenUrl(dto.getImagenUrl());
        producto.setIngredientes(dto.getIngredientes());
        producto.setPrecio(dto.getPrecio());
        producto.setPuntosValor(dto.getPuntosValor());
        producto.setTipo(dto.getTipo());
        producto.setEsCombo(dto.getEsCombo());
        producto.setEstadoAprobacion(dto.getEstadoAprobacion());
        producto.setActivo(dto.getActivo());
        return producto;
    }
}

