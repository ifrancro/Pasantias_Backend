package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.combo.ComboCreateRequest;
import com.example.herbalife_clubes.dtos.combo.ComboDTO;
import com.example.herbalife_clubes.dtos.combo.ComboItemDTO;
import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.pedidos.PedidoComboSupport;
import com.example.herbalife_clubes.repositories.*;
import com.example.herbalife_clubes.services.ComboClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComboClubServiceImpl implements ComboClubService {

    private static final int MAX_ITEMS_POR_COMBO = 3;

    private final ComboRepository comboRepository;
    private final ClubRepository clubRepository;
    private final ProductoRepository productoRepository;
    private final SaborRepository saborRepository;
    private final ClubProductoRepository clubProductoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ComboDTO> getCombosByClub(Integer clubId) {
        return comboRepository.findByClubId(clubId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ComboDTO getCombo(Integer comboId) {
        Combo combo = comboRepository.findByIdWithItems(comboId)
                .orElseThrow(() -> new ResourceNotFoundException("Combo no encontrado con id: " + comboId));
        return toDTO(combo);
    }

    @Override
    @Transactional
    public ComboDTO createCombo(Integer clubId, ComboCreateRequest request) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));

        validarRequest(request);
        validarPrecio(request.getPrecio());

        Combo combo = new Combo();
        combo.setClub(club);
        combo.setNombre(request.getNombre().trim());
        combo.setDescripcion(request.getDescripcion());
        combo.setImagenUrl(request.getImagenUrl());
        combo.setPrecio(request.getPrecio());
        combo.setActivo(true);

        List<ComboItem> items = buildItems(combo, club, request.getItems());
        combo.setItems(items);
        combo.setPuntosValor(calcularPuntos(items));

        Combo saved = comboRepository.save(combo);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public ComboDTO updateCombo(Integer comboId, ComboCreateRequest request) {
        Combo combo = comboRepository.findByIdWithItems(comboId)
                .orElseThrow(() -> new ResourceNotFoundException("Combo no encontrado con id: " + comboId));

        validarRequest(request);
        validarPrecio(request.getPrecio());

        combo.setNombre(request.getNombre().trim());
        combo.setDescripcion(request.getDescripcion());
        combo.setImagenUrl(request.getImagenUrl());
        combo.setPrecio(request.getPrecio());

        combo.getItems().clear();
        List<ComboItem> newItems = buildItems(combo, combo.getClub(), request.getItems());
        combo.getItems().addAll(newItems);
        combo.setPuntosValor(calcularPuntos(newItems));

        Combo saved = comboRepository.save(combo);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public ComboDTO toggleCombo(Integer comboId) {
        Combo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new ResourceNotFoundException("Combo no encontrado con id: " + comboId));
        combo.setActivo(!combo.getActivo());
        return toDTO(comboRepository.save(combo));
    }

    @Override
    @Transactional
    public void deleteCombo(Integer comboId) {
        if (!comboRepository.existsById(comboId)) {
            throw new ResourceNotFoundException("Combo no encontrado con id: " + comboId);
        }
        comboRepository.deleteById(comboId);
    }

    private void validarRequest(ComboCreateRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del combo es requerido");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("El combo debe tener al menos un producto");
        }
        if (request.getItems().size() > MAX_ITEMS_POR_COMBO) {
            throw new IllegalArgumentException(
                    "Un combo no puede tener más de " + MAX_ITEMS_POR_COMBO + " productos distintos. "
                            + "Recibidos: " + request.getItems().size());
        }
    }

    private static void validarPrecio(BigDecimal precio) {
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(PedidoComboSupport.MENSAJE_PRECIO_COMBO_INVALIDO);
        }
    }

    private List<ComboItem> buildItems(
            Combo combo, Club club, List<ComboCreateRequest.ComboItemRequest> itemRequests) {
        List<ComboItem> items = new ArrayList<>();
        for (ComboCreateRequest.ComboItemRequest req : itemRequests) {
            if (req.getProductoId() == null) {
                throw new IllegalArgumentException("productoId es requerido para cada item del combo");
            }

            Producto producto = productoRepository.findById(req.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con id: " + req.getProductoId()));

            validarProductoEnCombo(producto, club);

            Sabor sabor = null;
            if (req.getSaborId() != null) {
                sabor = saborRepository.findById(req.getSaborId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Sabor no encontrado con id: " + req.getSaborId()));
            }

            ComboItem item = new ComboItem();
            item.setCombo(combo);
            item.setProducto(producto);
            item.setSabor(sabor);
            item.setCantidad(req.getCantidad() != null && req.getCantidad() > 0 ? req.getCantidad() : 1);
            items.add(item);
        }
        return items;
    }

    private void validarProductoEnCombo(Producto producto, Club club) {
        if (!"APROBADO".equalsIgnoreCase(producto.getEstadoAprobacion())) {
            throw new IllegalArgumentException(
                    "El producto " + producto.getNombre() + " no está aprobado");
        }
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new IllegalArgumentException(
                    "El producto " + producto.getNombre() + " no está activo");
        }

        String tipo = producto.getTipo() != null ? producto.getTipo().toUpperCase() : "";
        if ("LOCAL".equals(tipo)) {
            if (producto.getClubCreador() == null
                    || !producto.getClubCreador().getId().equals(club.getId())) {
                throw new IllegalArgumentException(
                        "No se puede incluir un producto local de otro club en el combo");
            }
        } else if ("GLOBAL".equals(tipo)) {
            if (club.getHub() == null || producto.getHub() == null
                    || !producto.getHub().getId().equals(club.getHub().getId())) {
                throw new IllegalArgumentException(
                        "El producto global " + producto.getNombre() + " no pertenece al hub del club");
            }
        } else {
            throw new IllegalArgumentException("Tipo de producto no soportado en combos: " + producto.getTipo());
        }
    }

    private static int calcularPuntos(List<ComboItem> items) {
        return items.stream()
                .mapToInt(item -> {
                    int pv = item.getProducto().getPuntosValor() != null ? item.getProducto().getPuntosValor() : 0;
                    return pv * item.getCantidad();
                })
                .sum();
    }

    private ComboDTO toDTO(Combo combo) {
        List<ComboItemDTO> itemDTOs = combo.getItems() != null
                ? combo.getItems().stream().map(this::toItemDTO).collect(Collectors.toList())
                : List.of();

        return ComboDTO.builder()
                .id(combo.getId())
                .clubId(combo.getClub().getId())
                .clubNombre(combo.getClub().getNombreClub())
                .nombre(combo.getNombre())
                .descripcion(combo.getDescripcion())
                .imagenUrl(combo.getImagenUrl())
                .puntosValor(combo.getPuntosValor())
                .precio(combo.getPrecio())
                .activo(combo.getActivo())
                .disponible(esComboDisponible(combo))
                .items(itemDTOs)
                .build();
    }

    private boolean esComboDisponible(Combo combo) {
        if (!Boolean.TRUE.equals(combo.getActivo())) {
            return false;
        }
        if (combo.getPrecio() == null || combo.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (combo.getItems() == null || combo.getItems().isEmpty()) {
            return false;
        }
        Integer clubId = combo.getClub().getId();
        for (ComboItem item : combo.getItems()) {
            Producto producto = item.getProducto();
            if (!"APROBADO".equalsIgnoreCase(producto.getEstadoAprobacion())) {
                return false;
            }
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                return false;
            }
            var cp = clubProductoRepository.findByClubIdAndProductoId(clubId, producto.getId());
            if (cp.isEmpty() || cp.get().getDisponible() == null || !cp.get().getDisponible()) {
                return false;
            }
        }
        return true;
    }

    private ComboItemDTO toItemDTO(ComboItem item) {
        return ComboItemDTO.builder()
                .id(item.getId())
                .productoId(item.getProducto().getId())
                .productoNombre(item.getProducto().getNombre())
                .productoImagenUrl(item.getProducto().getImagenUrl())
                .puntosValorProducto(item.getProducto().getPuntosValor())
                .saborId(item.getSabor() != null ? item.getSabor().getId() : null)
                .saborNombre(item.getSabor() != null ? item.getSabor().getNombre() : null)
                .cantidad(item.getCantidad())
                .build();
    }
}
