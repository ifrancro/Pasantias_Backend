package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.entities.Pedido;

public class PedidoMapper {
    public static PedidoDTO mapPedidoToPedidoDTO(Pedido pedido) {
        PedidoDTO dto = new PedidoDTO();
        dto.setId(pedido.getId());
        dto.setMembresiaId(pedido.getMembresia() != null ? pedido.getMembresia().getId() : null);
        dto.setMembresiaNumeroSocio(pedido.getMembresia() != null ? pedido.getMembresia().getNumeroSocio() : null);
        dto.setClubId(pedido.getClub() != null ? pedido.getClub().getId() : null);
        dto.setClubNombre(pedido.getClub() != null ? pedido.getClub().getNombreClub() : null);
        // Compatibilidad: si el pedido tiene items, exponer el primero en productoId/cantidad
        if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
            var item = pedido.getItems().get(0);
            dto.setProductoId(item.getProducto() != null ? item.getProducto().getId() : null);
            dto.setProductoNombre(item.getProducto() != null ? item.getProducto().getNombre() : null);
            dto.setCantidad(item.getCantidad());
        }
        dto.setHorarioDeseado(pedido.getHorarioDeseado());
        dto.setTipoConsumo(pedido.getTipoConsumo() != null ? pedido.getTipoConsumo().name() : null);
        dto.setObservaciones(pedido.getObservaciones());
        dto.setEstado(pedido.getEstado() != null ? pedido.getEstado().name() : null);
        dto.setFechaPedido(pedido.getFechaPedido());
        return dto;
    }

    public static Pedido mapPedidoDTOToPedido(PedidoDTO dto) {
        Pedido pedido = new Pedido();
        pedido.setId(dto.getId());
        pedido.setHorarioDeseado(dto.getHorarioDeseado());
        pedido.setObservaciones(dto.getObservaciones());
        return pedido;
    }
}

