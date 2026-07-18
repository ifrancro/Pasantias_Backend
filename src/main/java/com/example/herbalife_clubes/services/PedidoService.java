package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoMostradorRequestDTO;
import com.example.herbalife_clubes.common.PagedResponse;

import java.time.LocalDateTime;
import java.util.List;

import com.example.herbalife_clubes.dtos.pedido.PedidoConItemsDTO;

public interface PedidoService {
    PedidoDTO createPedido(PedidoDTO pedidoDTO, Integer membresiaId, Integer clubId, Integer productoId);
    PedidoDTO createPedidoConItems(PedidoConItemsDTO pedidoDTO, Integer membresiaId, Integer clubId);
    PedidoDTO createPedidoMostrador(PedidoMostradorRequestDTO request);
    PedidoDTO getPedido(Integer pedidoId);
    List<PedidoDTO> getPedidosBySocio(Integer membresiaId);
    List<PedidoDTO> getPedidosByClub(Integer clubId);

    PagedResponse<PedidoDTO> getPedidosByClubPaginados(
            Integer clubId, int page, int size, String estado, LocalDateTime desde, LocalDateTime hasta);

    PagedResponse<PedidoDTO> getPedidosBySocioPaginados(
            Integer membresiaId, int page, int size, String estado, LocalDateTime desde, LocalDateTime hasta);

    PedidoDTO actualizarEstado(Integer pedidoId, String estado, Integer tiempoEstimadoMinutos);
    PedidoDTO cancelarPedido(Integer pedidoId);
}

