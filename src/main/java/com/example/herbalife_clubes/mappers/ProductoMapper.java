package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.entities.Producto;

public class ProductoMapper {
    public static ProductoDTO mapProductoToProductoDTO(Producto producto) {
        ProductoDTO dto = new ProductoDTO();
        dto.setId(producto.getId());
        dto.setHubId(producto.getHub() != null ? producto.getHub().getId() : null);
        dto.setHubNombre(producto.getHub() != null ? producto.getHub().getNombre() : null);
        dto.setNombre(producto.getNombre());
        dto.setDescripcion(producto.getDescripcion());
        dto.setActivo(producto.getActivo());
        dto.setCreatedAt(producto.getCreatedAt());
        return dto;
    }

    public static Producto mapProductoDTOToProducto(ProductoDTO dto) {
        Producto producto = new Producto();
        producto.setId(dto.getId());
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setActivo(dto.getActivo());
        return producto;
    }
}

