package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.entities.EstadoPedido;
import com.example.herbalife_clubes.entities.Pedido;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class PedidoMapperFechaPedidoTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void fechaPedidoLegacySeExponeComoInstantUtc() {
        Pedido pedido = new Pedido();
        pedido.setId(1);
        pedido.setEstado(EstadoPedido.RECIBIDO);
        pedido.setFechaPedido(LocalDateTime.of(2026, 8, 31, 19, 0, 0));

        var dto = PedidoMapper.mapPedidoToPedidoDTO(pedido);

        assertEquals(Instant.parse("2026-08-31T19:00:00Z"), dto.getFechaPedido());
    }

    @Test
    void fechaPedidoEnJsonTieneZonaExplicita() throws Exception {
        Pedido pedido = new Pedido();
        pedido.setId(2);
        pedido.setEstado(EstadoPedido.RECIBIDO);
        pedido.setFechaPedido(LocalDateTime.of(2026, 8, 31, 19, 0, 0));

        var dto = PedidoMapper.mapPedidoToPedidoDTO(pedido);
        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"fechaPedido\":\"2026-08-31T19:00:00Z\""));
    }

    @Test
    void serializacionInterpretaNaiveComoUtc() throws Exception {
        Pedido pedido = new Pedido();
        pedido.setId(3);
        pedido.setEstado(EstadoPedido.RECIBIDO);
        pedido.setFechaPedido(LocalDateTime.of(2026, 8, 31, 19, 0, 0));

        var dto = PedidoMapper.mapPedidoToPedidoDTO(pedido);
        String json = objectMapper.writeValueAsString(dto);

        assertEquals(Instant.parse("2026-08-31T19:00:00Z"), dto.getFechaPedido());
        assertTrue(json.contains("2026-08-31T19:00:00Z"));
    }
}
