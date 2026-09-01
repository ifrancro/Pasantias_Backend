package com.example.herbalife_clubes.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class PedidoFechaPedidoTest {

    @Test
    void prePersistAsignaFechaUtcCuandoEsNull() {
        Pedido pedido = new Pedido();
        pedido.onCreate();

        LocalDateTime reference = LocalDateTime.now(ZoneOffset.UTC);
        assertNotNull(pedido.getFechaPedido());
        assertTrue(
                pedido.getFechaPedido().isAfter(reference.minusSeconds(2))
                        && pedido.getFechaPedido().isBefore(reference.plusSeconds(2)));
    }

    @Test
    void prePersistNoSobreescribeFechaExistente() {
        Pedido pedido = new Pedido();
        LocalDateTime fija = LocalDateTime.of(2026, 1, 15, 12, 30, 0);
        pedido.setFechaPedido(fija);
        pedido.onCreate();
        assertEquals(fija, pedido.getFechaPedido());
    }
}
