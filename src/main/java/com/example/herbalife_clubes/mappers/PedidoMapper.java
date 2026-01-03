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
        dto.setProductoId(pedido.getProducto() != null ? pedido.getProducto().getId() : null);
        dto.setProductoNombre(pedido.getProducto() != null ? pedido.getProducto().getNombre() : null);
        dto.setCantidad(pedido.getCantidad());
        dto.setHorarioDeseado(pedido.getHorarioDeseado());
        dto.setTipoConsumo(pedido.getTipoConsumo());
        dto.setObservaciones(pedido.getObservaciones());
        dto.setEstado(pedido.getEstado());
        dto.setFechaCreacion(pedido.getFechaCreacion());
        dto.setFechaActualizacion(pedido.getFechaActualizacion());
        return dto;
    }

    public static Pedido mapPedidoDTOToPedido(PedidoDTO dto) {
        Pedido pedido = new Pedido();
        pedido.setId(dto.getId());
        pedido.setCantidad(dto.getCantidad());
        pedido.setHorarioDeseado(dto.getHorarioDeseado());
        pedido.setTipoConsumo(dto.getTipoConsumo());
        pedido.setObservaciones(dto.getObservaciones());
        pedido.setEstado(dto.getEstado());
        return pedido;
    }
}

