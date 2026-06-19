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
    public void asignarSaborAProducto(Integer productoId, Integer saborId, Usuario usuario) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        Sabor sabor = saborRepository.findById(saborId)
                .orElseThrow(() -> new ResourceNotFoundException("Sabor no encontrado con id: " + saborId));

        validarPuedeGestionarSaboresDeProducto(usuario, producto);
        validarSaborDelMismoHub(producto, sabor);

        if (!productoSaborRepository.existsByProductoIdAndSaborId(productoId, saborId)) {
            ProductoSabor ps = new ProductoSabor();
            ps.setProducto(producto);
            ps.setSabor(sabor);
            productoSaborRepository.save(ps);
        }

        if (esAnfitrion(usuario)) {
            Club club = obtenerClubAnfitrion(usuario);
            activarSaborEnClub(club, producto, sabor, true);
        }
    }

    @Override
    @Transactional
    public void quitarSaborDeProducto(Integer productoId, Integer saborId, Usuario usuario) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));

        validarPuedeGestionarSaboresDeProducto(usuario, producto);

        productoSaborRepository.findByProductoIdAndSaborId(productoId, saborId)
                .ifPresent(productoSaborRepository::delete);

        if (esAnfitrion(usuario)) {
            Club club = obtenerClubAnfitrion(usuario);
            clubProductoSaborRepository
                    .findByClubIdAndProductoIdAndSaborId(club.getId(), productoId, saborId)
                    .ifPresent(clubProductoSaborRepository::delete);
        }
    }

    // ===================== Disponibilidad por club (Anfitrión) =====================

    @Override
    @Transactional(readOnly = true)
    public List<SaborDisponibilidadDTO> getSaboresDeProductoEnClub(Integer clubId, Integer productoId) {
        List<ProductoSabor> productoSabores = productoSaborRepository.findByProductoId(productoId);

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
    @Transactional(readOnly = true)
    public List<SaborDisponibilidadDTO> getSaboresGestionEnClub(Integer clubId, Integer productoId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));

        if (club.getHub() == null) {
            throw new IllegalArgumentException("El club no tiene Hub asociado");
        }

        Map<Integer, Boolean> dispMap = clubProductoSaborRepository
                .findByClubIdAndProductoId(clubId, productoId)
                .stream()
                .collect(Collectors.toMap(
                        cps -> cps.getSabor().getId(),
                        cps -> Boolean.TRUE.equals(cps.getDisponible())
                ));

        return saborRepository.findByHubId(club.getHub().getId()).stream()
                .filter(s -> s.getActivo() == null || s.getActivo())
                .map(s -> SaborDisponibilidadDTO.builder()
                        .id(s.getId())
                        .nombre(s.getNombre())
                        .disponible(dispMap.getOrDefault(s.getId(), false))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SaborDisponibilidadDTO toggleSaborEnClub(Integer clubId, Integer productoId, Integer saborId, Usuario usuario) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        Sabor sabor = saborRepository.findById(saborId)
                .orElseThrow(() -> new ResourceNotFoundException("Sabor no encontrado con id: " + saborId));

        validarPuedeGestionarSaboresEnClub(usuario, club, producto);
        validarSaborDelMismoHub(producto, sabor);

        ClubProductoSabor cps = clubProductoSaborRepository
                .findByClubIdAndProductoIdAndSaborId(clubId, productoId, saborId)
                .orElse(null);

        boolean nuevoEstado = cps == null || !Boolean.TRUE.equals(cps.getDisponible());

        if (nuevoEstado) {
            if (!productoSaborRepository.existsByProductoIdAndSaborId(productoId, saborId)) {
                ProductoSabor ps = new ProductoSabor();
                ps.setProducto(producto);
                ps.setSabor(sabor);
                productoSaborRepository.save(ps);
            }
        }

        activarSaborEnClub(club, producto, sabor, nuevoEstado);

        return SaborDisponibilidadDTO.builder()
                .id(sabor.getId())
                .nombre(sabor.getNombre())
                .disponible(nuevoEstado)
                .build();
    }

    // ===================== Helpers =====================

    private boolean esAdmin(Usuario usuario) {
        String rol = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        return "ADMIN".equalsIgnoreCase(rol);
    }

    private boolean esAnfitrion(Usuario usuario) {
        String rol = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        return "ANFITRION".equalsIgnoreCase(rol);
    }

    private Club obtenerClubAnfitrion(Usuario usuario) {
        List<Club> clubes = clubRepository.findByAnfitrionId(usuario.getId());
        if (clubes.isEmpty()) {
            throw new IllegalArgumentException("El anfitrión no tiene un club asociado");
        }
        return clubes.get(0);
    }

    private void validarPuedeGestionarSaboresDeProducto(Usuario usuario, Producto producto) {
        if (esAdmin(usuario)) {
            return;
        }
        if (!esAnfitrion(usuario)) {
            throw new IllegalArgumentException("No tienes permiso para gestionar sabores de este producto");
        }
        validarProductoDelAnfitrion(usuario, producto);
    }

    private void validarPuedeGestionarSaboresEnClub(Usuario usuario, Club club, Producto producto) {
        if (esAdmin(usuario)) {
            return;
        }
        if (!esAnfitrion(usuario)) {
            throw new IllegalArgumentException("No tienes permiso para gestionar sabores en este club");
        }
        if (club.getAnfitrion() == null || !club.getAnfitrion().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("No eres el anfitrión de este club");
        }
        validarProductoDelAnfitrion(usuario, producto);
    }

    private void validarProductoDelAnfitrion(Usuario usuario, Producto producto) {
        if ("LOCAL".equalsIgnoreCase(producto.getTipo())) {
            Club creador = producto.getClubCreador();
            if (creador == null || creador.getAnfitrion() == null
                    || !creador.getAnfitrion().getId().equals(usuario.getId())) {
                throw new IllegalArgumentException("No puedes gestionar sabores de este producto local");
            }
            return;
        }
        Club club = obtenerClubAnfitrion(usuario);
        if (producto.getHub() == null || club.getHub() == null
                || !producto.getHub().getId().equals(club.getHub().getId())) {
            throw new IllegalArgumentException("El producto no pertenece al hub de tu club");
        }
    }

    private void validarSaborDelMismoHub(Producto producto, Sabor sabor) {
        if (producto.getHub() == null || sabor.getHub() == null
                || !producto.getHub().getId().equals(sabor.getHub().getId())) {
            throw new IllegalArgumentException("El sabor no pertenece al mismo hub que el producto");
        }
    }

    private void activarSaborEnClub(Club club, Producto producto, Sabor sabor, boolean disponible) {
        ClubProductoSabor cps = clubProductoSaborRepository
                .findByClubIdAndProductoIdAndSaborId(club.getId(), producto.getId(), sabor.getId())
                .orElse(null);

        if (cps == null) {
            cps = new ClubProductoSabor();
            cps.setClub(club);
            cps.setProducto(producto);
            cps.setSabor(sabor);
        }
        cps.setDisponible(disponible);
        clubProductoSaborRepository.save(cps);
    }

    private SaborDTO toDTO(Sabor sabor) {
        return SaborDTO.builder()
                .id(sabor.getId())
                .hubId(sabor.getHub().getId())
                .nombre(sabor.getNombre())
                .activo(sabor.getActivo())
                .build();
    }
}
