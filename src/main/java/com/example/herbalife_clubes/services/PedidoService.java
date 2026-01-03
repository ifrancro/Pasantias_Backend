package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;

import java.util.List;

public interface PedidoService {
    PedidoDTO createPedido(PedidoDTO pedidoDTO, Integer membresiaId, Integer clubId, Integer productoId);
    PedidoDTO getPedido(Integer pedidoId);
    List<PedidoDTO> getPedidosBySocio(Integer membresiaId);
    List<PedidoDTO> getPedidosByClub(Integer clubId);
    PedidoDTO actualizarEstado(Integer pedidoId, String estado);
    PedidoDTO cancelarPedido(Integer pedidoId);
}

