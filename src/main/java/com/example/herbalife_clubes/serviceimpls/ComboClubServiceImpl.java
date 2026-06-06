package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.combo.ComboCreateRequest;
import com.example.herbalife_clubes.dtos.combo.ComboDTO;
import com.example.herbalife_clubes.dtos.combo.ComboItemDTO;
import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.*;
import com.example.herbalife_clubes.services.ComboClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComboClubServiceImpl implements ComboClubService {

    private static final int MAX_ITEMS_POR_COMBO = 3;

    private final ComboRepository comboRepository;
    private final ComboItemRepository comboItemRepository;
    private final ClubRepository clubRepository;
    private final ProductoRepository productoRepository;
    private final SaborRepository saborRepository;

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
        Combo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new ResourceNotFoundException("Combo no encontrado con id: " + comboId));
        return toDTO(combo);
    }

    @Override
    @Transactional
    public ComboDTO createCombo(Integer clubId, ComboCreateRequest request) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));

        validarRequest(request);

        Combo combo = new Combo();
        combo.setClub(club);
        combo.setNombre(request.getNombre().trim());
        combo.setDescripcion(request.getDescripcion());
        combo.setImagenUrl(request.getImagenUrl());
        combo.setActivo(true);

        // Crear items
        List<ComboItem> items = buildItems(combo, request.getItems());
        combo.setItems(items);

        // Calcular puntos: si no se proporcionan, sumar los puntos de los productos
        if (request.getPuntosValor() != null) {
            combo.setPuntosValor(request.getPuntosValor());
        } else {
            int totalPuntos = items.stream()
                    .mapToInt(item -> {
                        int pv = item.getProducto().getPuntosValor() != null ? item.getProducto().getPuntosValor() : 0;
                        return pv * item.getCantidad();
                    })
                    .sum();
            combo.setPuntosValor(totalPuntos);
        }

        Combo saved = comboRepository.save(combo);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public ComboDTO updateCombo(Integer comboId, ComboCreateRequest request) {
        Combo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new ResourceNotFoundException("Combo no encontrado con id: " + comboId));

        validarRequest(request);

        combo.setNombre(request.getNombre().trim());
        combo.setDescripcion(request.getDescripcion());
        combo.setImagenUrl(request.getImagenUrl());

        // Reemplazar items
        combo.getItems().clear();
        List<ComboItem> newItems = buildItems(combo, request.getItems());
        combo.getItems().addAll(newItems);

        // Recalcular puntos
        if (request.getPuntosValor() != null) {
            combo.setPuntosValor(request.getPuntosValor());
        } else {
            int totalPuntos = newItems.stream()
                    .mapToInt(item -> {
                        int pv = item.getProducto().getPuntosValor() != null ? item.getProducto().getPuntosValor() : 0;
                        return pv * item.getCantidad();
                    })
                    .sum();
            combo.setPuntosValor(totalPuntos);
        }

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

    // ===================== Helpers =====================

    private void validarRequest(ComboCreateRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del combo es requerido");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("El combo debe tener al menos un producto");
        }
        if (request.getItems().size() > MAX_ITEMS_POR_COMBO) {
            throw new IllegalArgumentException(
                    "Un combo no puede tener más de " + MAX_ITEMS_POR_COMBO + " productos distintos. " +
                    "Recibidos: " + request.getItems().size());
        }
    }

    private List<ComboItem> buildItems(Combo combo, List<ComboCreateRequest.ComboItemRequest> itemRequests) {
        List<ComboItem> items = new ArrayList<>();
        for (ComboCreateRequest.ComboItemRequest req : itemRequests) {
            if (req.getProductoId() == null) {
                throw new IllegalArgumentException("productoId es requerido para cada item del combo");
            }

            Producto producto = productoRepository.findById(req.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con id: " + req.getProductoId()));

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
                .activo(combo.getActivo())
                .items(itemDTOs)
                .build();
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
