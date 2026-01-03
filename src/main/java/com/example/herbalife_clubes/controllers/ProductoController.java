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

    @PostMapping
    public ResponseEntity<ProductoDTO> createProducto(@RequestBody ProductoDTO productoDTO,
                                                       @RequestParam Integer clubId) {
        ProductoDTO savedProductoDTO = productoService.createProducto(productoDTO, clubId);
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
}

