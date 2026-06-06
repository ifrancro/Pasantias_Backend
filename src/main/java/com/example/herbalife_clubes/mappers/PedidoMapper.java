package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemDTO;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.PedidoItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PedidoMapper {
    public static PedidoDTO mapPedidoToPedidoDTO(Pedido pedido) {
        PedidoDTO dto = new PedidoDTO();
        dto.setId(pedido.getId());
        dto.setMembresiaId(pedido.getMembresia() != null ? pedido.getMembresia().getId() : null);
        dto.setMembresiaNumeroSocio(pedido.getMembresia() != null ? pedido.getMembresia().getNumeroSocio() : null);
        dto.setClubId(pedido.getClub() != null ? pedido.getClub().getId() : null);
        dto.setClubNombre(pedido.getClub() != null ? pedido.getClub().getNombreClub() : null);
        
        // Mapear todos los items del pedido
        List<PedidoItemDTO> itemsDTO = new ArrayList<>();
        if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
            itemsDTO = pedido.getItems().stream()
                    .map(item -> {
                        PedidoItemDTO itemDTO = new PedidoItemDTO();
                        itemDTO.setProductoId(item.getProducto() != null ? item.getProducto().getId() : null);
                        itemDTO.setProductoNombre(item.getProducto() != null ? item.getProducto().getNombre() : null);
                        itemDTO.setCantidad(item.getCantidad());
                        itemDTO.setNota(item.getNota());
                        itemDTO.setPrecioUnitario(item.getPrecioUnitario());
                        itemDTO.setSubtotal(item.getSubtotal());
                        itemDTO.setComboId(item.getCombo() != null ? item.getCombo().getId() : null);
                        itemDTO.setComboNombre(item.getCombo() != null ? item.getCombo().getNombre() : null);
                        return itemDTO;
                    })
                    .collect(Collectors.toList());
        }
        dto.setItems(itemsDTO);
        
        // Compatibilidad: si el pedido tiene items, exponer el primero en productoId/cantidad
        if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
            var item = pedido.getItems().get(0);
            dto.setProductoId(item.getProducto() != null ? item.getProducto().getId() : null);
            dto.setProductoNombre(item.getProducto() != null ? item.getProducto().getNombre() : null);
            dto.setCantidad(item.getCantidad());
        }
        // horarioDeseado fue eliminado - mantener null para compatibilidad
        dto.setHorarioDeseado(null);
        dto.setTipoConsumo(pedido.getTipoConsumo() != null ? pedido.getTipoConsumo().name() : null);
        dto.setTiempoEstimadoMinutos(pedido.getTiempoEstimadoMinutos());
        dto.setObservaciones(pedido.getObservaciones());
        dto.setEstado(pedido.getEstado() != null ? pedido.getEstado().name() : null);
        dto.setFechaPedido(pedido.getFechaPedido());
        return dto;
    }

    public static Pedido mapPedidoDTOToPedido(PedidoDTO dto) {
        Pedido pedido = new Pedido();
        pedido.setId(dto.getId());
        // horarioDeseado fue eliminado - ahora se usa tiempoEstimadoMinutos
        pedido.setTiempoEstimadoMinutos(dto.getTiempoEstimadoMinutos());
        pedido.setObservaciones(dto.getObservaciones());
        return pedido;
    }
}

