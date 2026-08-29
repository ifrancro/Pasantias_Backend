package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.common.PagedResponse;
import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.entities.EstadoPedido;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.repositories.PedidoRepository;
import com.example.herbalife_clubes.serviceimpls.PedidoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoPaginationServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    @Test
    void clubPageUsesPageableAndBatchLoadOnce() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Integer> idPage = new PageImpl<>(List.of(10, 11), pageable, 2);
        when(pedidoRepository.findIdsByClubId(
                eq(1), eq(false), any(EstadoPedido.class),
                eq(false), any(LocalDateTime.class),
                eq(false), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(idPage);

        Pedido p10 = stubPedido(10);
        Pedido p11 = stubPedido(11);
        when(pedidoRepository.findWithRelationsByIds(List.of(10, 11)))
                .thenReturn(List.of(p11, p10));

        PagedResponse<PedidoDTO> result =
                pedidoService.getPedidosByClubPaginados(1, 0, 20, null, null, null);

        assertEquals(2, result.content().size());
        assertEquals(10, result.content().get(0).getId());
        assertEquals(11, result.content().get(1).getId());
        assertEquals(2, result.totalElements());
        assertFalse(result.hasNext());

        verify(pedidoRepository, times(1)).findWithRelationsByIds(anyList());
    }

    @Test
    void emptyPageDoesNotLoadRelations() {
        when(pedidoRepository.findIdsByClubId(
                eq(1), eq(false), any(EstadoPedido.class),
                eq(false), any(LocalDateTime.class),
                eq(false), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        PagedResponse<PedidoDTO> result =
                pedidoService.getPedidosByClubPaginados(1, 0, 20, null, null, null);

        assertTrue(result.content().isEmpty());
        verify(pedidoRepository, never()).findWithRelationsByIds(anyList());
    }

    @Test
    void absentFiltersPassFalseFlags() {
        when(pedidoRepository.findIdsByClubId(
                eq(1), eq(false), eq(EstadoPedido.RECIBIDO),
                eq(false), any(LocalDateTime.class),
                eq(false), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        pedidoService.getPedidosByClubPaginados(1, 0, 20, null, null, null);

        verify(pedidoRepository).findIdsByClubId(
                eq(1), eq(false), eq(EstadoPedido.RECIBIDO),
                eq(false), any(LocalDateTime.class),
                eq(false), any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void presentFiltersPassTrueFlags() {
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 2, 1, 0, 0);
        when(pedidoRepository.findIdsByClubId(
                eq(1), eq(true), eq(EstadoPedido.RECIBIDO),
                eq(true), eq(desde),
                eq(true), eq(hasta), any(Pageable.class)))
                .thenReturn(Page.empty());

        pedidoService.getPedidosByClubPaginados(1, 0, 20, "RECIBIDO", desde, hasta);

        verify(pedidoRepository).findIdsByClubId(
                eq(1), eq(true), eq(EstadoPedido.RECIBIDO),
                eq(true), eq(desde),
                eq(true), eq(hasta), any(Pageable.class));
    }

    @Test
    void rejectsInvalidEstado() {
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.getPedidosByClubPaginados(1, 0, 20, "NOEXISTE", null, null));
    }

    @Test
    void rejectsInvertedDateRange() {
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.getPedidosByClubPaginados(
                        1, 0, 20, null,
                        java.time.LocalDateTime.of(2026, 2, 1, 0, 0),
                        java.time.LocalDateTime.of(2026, 1, 1, 0, 0)));
    }

    @Test
    void capsSizeToMax() {
        when(pedidoRepository.findIdsByMembresiaId(
                eq(5), eq(false), any(EstadoPedido.class),
                eq(false), any(LocalDateTime.class),
                eq(false), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        pedidoService.getPedidosBySocioPaginados(5, 0, 999, null, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(pedidoRepository).findIdsByMembresiaId(
                eq(5), eq(false), any(EstadoPedido.class),
                eq(false), any(LocalDateTime.class),
                eq(false), any(LocalDateTime.class), captor.capture());
        assertEquals(100, captor.getValue().getPageSize());
    }

    @Test
    void rejectsNegativePage() {
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.getPedidosByClubPaginados(1, -1, 20, null, null, null));
    }

    private static Pedido stubPedido(int id) {
        Pedido p = mock(Pedido.class);
        when(p.getId()).thenReturn(id);
        when(p.getItems()).thenReturn(List.of());
        when(p.getMembresia()).thenReturn(null);
        when(p.getClub()).thenReturn(null);
        when(p.getTipoConsumo()).thenReturn(null);
        when(p.getEstado()).thenReturn(null);
        when(p.getFechaPedido()).thenReturn(null);
        when(p.getObservaciones()).thenReturn(null);
        when(p.getTipoPago()).thenReturn(null);
        when(p.getTiempoEstimadoMinutos()).thenReturn(null);
        return p;
    }
}
