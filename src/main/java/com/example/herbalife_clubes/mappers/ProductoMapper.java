package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.entities.Producto;

public class ProductoMapper {
    public static ProductoDTO mapProductoToProductoDTO(Producto producto) {
        ProductoDTO dto = new ProductoDTO();
        dto.setId(producto.getId());
        dto.setClubId(producto.getClub() != null ? producto.getClub().getId() : null);
        dto.setClubNombre(producto.getClub() != null ? producto.getClub().getNombreClub() : null);
        dto.setNombre(producto.getNombre());
        dto.setDescripcion(producto.getDescripcion());
        dto.setPrecioReferencial(producto.getPrecioReferencial());
        dto.setActivo(producto.getActivo());
        dto.setCreatedAt(producto.getCreatedAt());
        return dto;
    }

    public static Producto mapProductoDTOToProducto(ProductoDTO dto) {
        Producto producto = new Producto();
        producto.setId(dto.getId());
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setPrecioReferencial(dto.getPrecioReferencial());
        producto.setActivo(dto.getActivo());
        return producto;
    }
}

