package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoConDisponibilidadDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.ProductoMapper;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.services.ProductoService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductoServiceImpl implements ProductoService {
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private HubRepository hubRepository;
    @Autowired
    private ClubProductoRepository clubProductoRepository;

    @Override
    public ProductoDTO createProducto(ProductoDTO productoDTO, Integer clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        Hub hub = club.getHub();
        if (hub == null) {
            throw new IllegalArgumentException("El club no tiene HUB asociado");
        }

        Producto producto = ProductoMapper.mapProductoDTOToProducto(productoDTO);
        producto.setHub(hub);
        if (producto.getActivo() == null) {
            producto.setActivo(true);
        }
        
        Producto savedProducto = productoRepository.save(producto);

        // Crear relación ClubProducto automáticamente (comportamiento legacy)
        clubProductoRepository.findByClubIdAndProductoId(clubId, savedProducto.getId())
                .orElseGet(() -> {
                    ClubProducto cp = new ClubProducto();
                    cp.setClub(club);
                    cp.setProducto(savedProducto);
                    cp.setDisponible(false);
                    return clubProductoRepository.save(cp);
                });

        return ProductoMapper.mapProductoToProductoDTO(savedProducto);
    }

    @Override
    public ProductoDTO createProductoFromHub(ProductoDTO productoDTO, Integer hubId) {
        // Validar que el hub existe
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResourceNotFoundException("Hub no encontrado con id: " + hubId));

        // Crear producto desde el DTO
        Producto producto = ProductoMapper.mapProductoDTOToProducto(productoDTO);
        producto.setHub(hub);
        
        // Si no viene activo, por defecto true
        if (producto.getActivo() == null) {
            producto.setActivo(true);
        }
        
        // Guardar producto (NO se crea relación ClubProducto automáticamente)
        // Los clubs verán el producto en GET /api/productos/hub/{hubId}?clubId={clubId}
        // y podrán habilitarlo usando el toggle
        Producto savedProducto = productoRepository.save(producto);

        return ProductoMapper.mapProductoToProductoDTO(savedProducto);
    }

    @Override
    public ProductoDTO updateProducto(Integer productoId, ProductoDTO productoDTO) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        
        producto.setNombre(productoDTO.getNombre());
        producto.setDescripcion(productoDTO.getDescripcion());
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
        return clubProductoRepository.findByClubIdAndDisponibleTrue(clubId).stream()
                .map(ClubProducto::getProducto)
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

    @Override
    public List<ProductoConDisponibilidadDTO> getProductosByHub(Integer hubId, Integer clubId) {
        // Obtener todos los productos del Hub (sin filtrar por activo ni disponibilidad)
        List<Producto> productos = productoRepository.findByHubId(hubId);
        
        return productos.stream()
                .map(producto -> {
                    // Buscar si existe relación en club_productos
                    Boolean disponible = null;
                    if (clubId != null) {
                        Optional<ClubProducto> clubProductoOpt = 
                                clubProductoRepository.findByClubIdAndProductoId(clubId, producto.getId());
                        disponible = clubProductoOpt.map(ClubProducto::getDisponible).orElse(null);
                    }
                    
                    return ProductoConDisponibilidadDTO.builder()
                            .id(producto.getId())
                            .hubId(producto.getHub() != null ? producto.getHub().getId() : null)
                            .hubNombre(producto.getHub() != null ? producto.getHub().getNombre() : null)
                            .nombre(producto.getNombre())
                            .descripcion(producto.getDescripcion())
                            .activo(producto.getActivo())
                            .disponible(disponible) // null si no hay relación, true/false si existe
                            .createdAt(producto.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public ProductoConDisponibilidadDTO toggleDisponibilidadEnClub(Integer clubId, Integer productoId) {
        // Validar que el club existe
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        
        // Validar que el producto existe
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        
        // Validar que el producto pertenece al mismo Hub que el club
        if (!producto.getHub().getId().equals(club.getHub().getId())) {
            throw new IllegalArgumentException("El producto no pertenece al mismo Hub que el club");
        }
        
        // Buscar o crear la relación en club_productos
        ClubProducto clubProducto = clubProductoRepository
                .findByClubIdAndProductoId(clubId, productoId)
                .orElseGet(() -> {
                    ClubProducto nuevo = new ClubProducto();
                    nuevo.setClub(club);
                    nuevo.setProducto(producto);
                    nuevo.setDisponible(false); // Por defecto false
                    return nuevo;
                });
        
        // Toggle del estado disponible (solo en club_productos, NO toca el activo global)
        clubProducto.setDisponible(!clubProducto.getDisponible());
        clubProducto = clubProductoRepository.save(clubProducto);
        
        // Construir y devolver el DTO
        return ProductoConDisponibilidadDTO.builder()
                .id(producto.getId())
                .hubId(producto.getHub() != null ? producto.getHub().getId() : null)
                .hubNombre(producto.getHub() != null ? producto.getHub().getNombre() : null)
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .activo(producto.getActivo()) // Estado global (no se modifica)
                .disponible(clubProducto.getDisponible()) // Estado local en el club
                .createdAt(producto.getCreatedAt())
                .build();
    }
}

