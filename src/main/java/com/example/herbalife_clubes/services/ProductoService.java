package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;

import java.util.List;

public interface ProductoService {
    ProductoDTO createProducto(ProductoDTO productoDTO, Integer clubId);
    ProductoDTO updateProducto(Integer productoId, ProductoDTO productoDTO);
    ProductoDTO getProducto(Integer productoId);
    List<ProductoDTO> getProductos();
    List<ProductoDTO> getProductosByClub(Integer clubId);
    ProductoDTO activarProducto(Integer productoId);
    ProductoDTO desactivarProducto(Integer productoId);
}

