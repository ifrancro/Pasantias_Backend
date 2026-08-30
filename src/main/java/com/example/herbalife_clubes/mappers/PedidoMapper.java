package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemOpcionResponseDTO;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.PedidoItem;
import com.example.herbalife_clubes.entities.PedidoItemOpcion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PedidoMapper {
    public static PedidoDTO mapPedidoToPedidoDTO(Pedido pedido) {
        inicializarOpcionesItems(pedido);

        PedidoDTO dto = new PedidoDTO();
        dto.setId(pedido.getId());
        dto.setMembresiaId(pedido.getMembresia() != null ? pedido.getMembresia().getId() : null);
        dto.setMembresiaNumeroSocio(pedido.getMembresia() != null ? pedido.getMembresia().getNumeroSocio() : null);
        dto.setClubId(pedido.getClub() != null ? pedido.getClub().getId() : null);
        dto.setClubNombre(pedido.getClub() != null ? pedido.getClub().getNombreClub() : null);

        List<PedidoItemDTO> itemsDTO = new ArrayList<>();
        if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
            itemsDTO = pedido.getItems().stream()
                    .map(PedidoMapper::mapItemToDto)
                    .collect(Collectors.toList());
        }
        dto.setItems(itemsDTO);

        if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
            PedidoItemDTO first = itemsDTO.get(0);
            dto.setProductoId(first.getProductoId());
            dto.setProductoNombre(first.getProductoNombre());
            dto.setCantidad(first.getCantidad());
            dto.setOpciones(first.getOpciones() != null ? new ArrayList<>(first.getOpciones()) : new ArrayList<>());
        } else {
            dto.setOpciones(new ArrayList<>());
        }

        dto.setHorarioDeseado(null);
        dto.setTipoConsumo(pedido.getTipoConsumo() != null ? pedido.getTipoConsumo().name() : null);
        dto.setTipoPago(pedido.getTipoPago());
        dto.setTiempoEstimadoMinutos(pedido.getTiempoEstimadoMinutos());
        dto.setObservaciones(pedido.getObservaciones());
        dto.setEstado(pedido.getEstado() != null ? pedido.getEstado().name() : null);
        dto.setFechaPedido(pedido.getFechaPedido());
        return dto;
    }

    private static PedidoItemDTO mapItemToDto(PedidoItem item) {
        PedidoItemDTO itemDTO = new PedidoItemDTO();
        itemDTO.setProductoId(item.getProducto() != null ? item.getProducto().getId() : null);
        itemDTO.setProductoNombre(item.getProducto() != null ? item.getProducto().getNombre() : null);
        itemDTO.setCantidad(item.getCantidad());
        itemDTO.setNota(item.getNota());
        itemDTO.setPrecioUnitario(item.getPrecioUnitario());
        itemDTO.setSubtotal(item.getSubtotal());
        itemDTO.setComboId(item.getCombo() != null ? item.getCombo().getId() : null);
        itemDTO.setComboNombre(item.getCombo() != null ? item.getCombo().getNombre() : null);
        itemDTO.setOpciones(mapOpcionesItem(item));
        return itemDTO;
    }

    static List<PedidoItemOpcionResponseDTO> mapOpcionesItem(PedidoItem item) {
        if (item.getOpciones() == null || item.getOpciones().isEmpty()) {
            return new ArrayList<>();
        }
        return item.getOpciones().stream()
                .sorted(Comparator
                        .comparing((PedidoItemOpcion o) -> o.getGrupoOrdenSnapshot() == null ? 0 : o.getGrupoOrdenSnapshot())
                        .thenComparing(o -> o.getOpcionOrdenSnapshot() == null ? 0 : o.getOpcionOrdenSnapshot())
                        .thenComparing(o -> o.getId() == null ? 0 : o.getId()))
                .map(PedidoMapper::mapOpcionEntityToDto)
                .collect(Collectors.toList());
    }

    private static PedidoItemOpcionResponseDTO mapOpcionEntityToDto(PedidoItemOpcion entity) {
        PedidoItemOpcionResponseDTO dto = new PedidoItemOpcionResponseDTO();
        dto.setGrupoId(entity.getGrupo() != null ? entity.getGrupo().getId() : null);
        dto.setGrupoNombre(entity.getGrupoNombreSnapshot());
        dto.setGrupoOrden(entity.getGrupoOrdenSnapshot());
        dto.setOpcionId(entity.getOpcion() != null ? entity.getOpcion().getId() : null);
        dto.setOpcionNombre(entity.getOpcionNombreSnapshot());
        dto.setOpcionOrden(entity.getOpcionOrdenSnapshot());
        dto.setCantidad(entity.getCantidad());
        return dto;
    }

    private static void inicializarOpcionesItems(Pedido pedido) {
        if (pedido.getItems() == null) {
            return;
        }
        for (PedidoItem item : pedido.getItems()) {
            if (item.getOpciones() != null) {
                item.getOpciones().size();
            }
        }
    }

    public static Pedido mapPedidoDTOToPedido(PedidoDTO dto) {
        Pedido pedido = new Pedido();
        pedido.setId(dto.getId());
        pedido.setTiempoEstimadoMinutos(dto.getTiempoEstimadoMinutos());
        pedido.setObservaciones(dto.getObservaciones());
        return pedido;
    }
}
