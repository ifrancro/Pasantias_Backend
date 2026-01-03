package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.ProductoMapper;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.services.ProductoService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductoServiceImpl implements ProductoService {
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private ClubRepository clubRepository;

    @Override
    public ProductoDTO createProducto(ProductoDTO productoDTO, Integer clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        
        Producto producto = ProductoMapper.mapProductoDTOToProducto(productoDTO);
        producto.setClub(club);
        if (producto.getActivo() == null) {
            producto.setActivo(true);
        }
        
        Producto savedProducto = productoRepository.save(producto);
        return ProductoMapper.mapProductoToProductoDTO(savedProducto);
    }

    @Override
    public ProductoDTO updateProducto(Integer productoId, ProductoDTO productoDTO) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        
        producto.setNombre(productoDTO.getNombre());
        producto.setDescripcion(productoDTO.getDescripcion());
        producto.setPrecioReferencial(productoDTO.getPrecioReferencial());
        producto.setActivo(productoDTO.getActivo());
        
        Producto updatedProducto = productoRepository.save(producto);
        return ProductoMapper.mapProductoToProductoDTO(updatedProducto);
    }

    @Override
    public ProductoDTO getProducto(Integer productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        return ProductoMapper.mapProductoToProductoDTO(producto);
    }

    @Override
    public List<ProductoDTO> getProductos() {
        List<Producto> productos = productoRepository.findAll();
        return productos.stream()
                .map(ProductoMapper::mapProductoToProductoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductoDTO> getProductosByClub(Integer clubId) {
        List<Producto> productos = productoRepository.findByClubId(clubId);
        return productos.stream()
                .map(ProductoMapper::mapProductoToProductoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductoDTO activarProducto(Integer productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        producto.setActivo(true);
        Producto updatedProducto = productoRepository.save(producto);
        return ProductoMapper.mapProductoToProductoDTO(updatedProducto);
    }

    @Override
    public ProductoDTO desactivarProducto(Integer productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        producto.setActivo(false);
        Producto updatedProducto = productoRepository.save(producto);
        return ProductoMapper.mapProductoToProductoDTO(updatedProducto);
    }
}

