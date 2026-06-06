package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.sabor.SaborDTO;
import com.example.herbalife_clubes.dtos.sabor.SaborDisponibilidadDTO;
import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.*;
import com.example.herbalife_clubes.services.SaborService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaborServiceImpl implements SaborService {

    private final SaborRepository saborRepository;
    private final ProductoSaborRepository productoSaborRepository;
    private final ClubProductoSaborRepository clubProductoSaborRepository;
    private final HubRepository hubRepository;
    private final ProductoRepository productoRepository;
    private final ClubRepository clubRepository;

    // ===================== Catálogo de sabores (Admin) =====================

    @Override
    @Transactional(readOnly = true)
    public List<SaborDTO> getSaboresByHub(Integer hubId) {
        return saborRepository.findByHubId(hubId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SaborDTO createSabor(SaborDTO dto) {
        if (dto.getHubId() == null) {
            throw new IllegalArgumentException("hubId es requerido");
        }
        Hub hub = hubRepository.findById(dto.getHubId())
                .orElseThrow(() -> new ResourceNotFoundException("Hub no encontrado con id: " + dto.getHubId()));

        String nombre = dto.getNombre() != null ? dto.getNombre().trim() : "";
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del sabor no puede estar vacío");
        }
        if (saborRepository.existsByHubIdAndNombreIgnoreCase(hub.getId(), nombre)) {
            throw new IllegalArgumentException("Ya existe un sabor con nombre '" + nombre + "' en este Hub");
        }

        Sabor sabor = new Sabor();
        sabor.setHub(hub);
        sabor.setNombre(nombre);
        sabor.setActivo(dto.getActivo() != null ? dto.getActivo() : true);

        return toDTO(saborRepository.save(sabor));
    }

    @Override
    @Transactional
    public SaborDTO updateSabor(Integer saborId, SaborDTO dto) {
        Sabor sabor = saborRepository.findById(saborId)
                .orElseThrow(() -> new ResourceNotFoundException("Sabor no encontrado con id: " + saborId));

        if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
            sabor.setNombre(dto.getNombre().trim());
        }
        if (dto.getActivo() != null) {
            sabor.setActivo(dto.getActivo());
        }

        return toDTO(saborRepository.save(sabor));
    }

    // ===================== Sabores asignados a un producto =====================

    @Override
    @Transactional(readOnly = true)
    public List<SaborDTO> getSaboresDeProducto(Integer productoId) {
        return productoSaborRepository.findByProductoId(productoId).stream()
                .map(ps -> toDTO(ps.getSabor()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void asignarSaborAProducto(Integer productoId, Integer saborId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        Sabor sabor = saborRepository.findById(saborId)
                .orElseThrow(() -> new ResourceNotFoundException("Sabor no encontrado con id: " + saborId));

        if (productoSaborRepository.existsByProductoIdAndSaborId(productoId, saborId)) {
            return; // ya asignado
        }

        ProductoSabor ps = new ProductoSabor();
        ps.setProducto(producto);
        ps.setSabor(sabor);
        productoSaborRepository.save(ps);
    }

    @Override
    @Transactional
    public void quitarSaborDeProducto(Integer productoId, Integer saborId) {
        productoSaborRepository.findByProductoIdAndSaborId(productoId, saborId)
                .ifPresent(productoSaborRepository::delete);
    }

    // ===================== Disponibilidad por club (Anfitrión) =====================

    @Override
    @Transactional(readOnly = true)
    public List<SaborDisponibilidadDTO> getSaboresDeProductoEnClub(Integer clubId, Integer productoId) {
        // Obtener todos los sabores asignados al producto
        List<ProductoSabor> productoSabores = productoSaborRepository.findByProductoId(productoId);

        // Obtener las disponibilidades que el club ya configuró para este producto
        List<ClubProductoSabor> clubDisps = clubProductoSaborRepository
                .findByClubIdAndProductoId(clubId, productoId);

        Map<Integer, Boolean> dispMap = clubDisps.stream()
                .collect(Collectors.toMap(
                        cps -> cps.getSabor().getId(),
                        ClubProductoSabor::getDisponible
                ));

        return productoSabores.stream()
                .filter(ps -> ps.getSabor().getActivo() != null && ps.getSabor().getActivo())
                .map(ps -> SaborDisponibilidadDTO.builder()
                        .id(ps.getSabor().getId())
                        .nombre(ps.getSabor().getNombre())
                        .disponible(dispMap.getOrDefault(ps.getSabor().getId(), false))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SaborDisponibilidadDTO toggleSaborEnClub(Integer clubId, Integer productoId, Integer saborId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        Sabor sabor = saborRepository.findById(saborId)
                .orElseThrow(() -> new ResourceNotFoundException("Sabor no encontrado con id: " + saborId));

        // Verificar que el sabor está asignado al producto
        if (!productoSaborRepository.existsByProductoIdAndSaborId(productoId, saborId)) {
            throw new IllegalArgumentException(
                    "El sabor '" + sabor.getNombre() + "' no está asignado al producto '" + producto.getNombre() + "'");
        }

        ClubProductoSabor cps = clubProductoSaborRepository
                .findByClubIdAndProductoIdAndSaborId(clubId, productoId, saborId)
                .orElse(null);

        if (cps == null) {
            // Primera vez: crear con disponible = true
            cps = new ClubProductoSabor();
            cps.setClub(club);
            cps.setProducto(producto);
            cps.setSabor(sabor);
            cps.setDisponible(true);
        } else {
            cps.setDisponible(!cps.getDisponible());
        }

        clubProductoSaborRepository.save(cps);

        return SaborDisponibilidadDTO.builder()
                .id(sabor.getId())
                .nombre(sabor.getNombre())
                .disponible(cps.getDisponible())
                .build();
    }

    // ===================== Helpers =====================

    private SaborDTO toDTO(Sabor sabor) {
        return SaborDTO.builder()
                .id(sabor.getId())
                .hubId(sabor.getHub().getId())
                .nombre(sabor.getNombre())
                .activo(sabor.getActivo())
                .build();
    }
}
