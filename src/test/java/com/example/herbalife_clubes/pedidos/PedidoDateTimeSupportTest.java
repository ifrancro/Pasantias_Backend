package com.example.herbalife_clubes.pedidos;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class PedidoDateTimeSupportTest {

    @Test
    void nowUtcUsaZoneOffsetUtc() {
        LocalDateTime utc = PedidoDateTimeSupport.nowUtc();
        LocalDateTime expected = LocalDateTime.now(ZoneOffset.UTC);
        assertTrue(
                utc.isAfter(expected.minusSeconds(2)) && utc.isBefore(expected.plusSeconds(2)),
                "nowUtc debe estar en UTC, no en la zona del JVM");
    }

    @Test
    void nowUtcCoincideConReferenciaUtcNoConLocalImplicito() {
        LocalDateTime utc = PedidoDateTimeSupport.nowUtc();
        LocalDateTime referenceUtc = LocalDateTime.now(ZoneOffset.UTC);
        assertTrue(
                utc.isAfter(referenceUtc.minusSeconds(2))
                        && utc.isBefore(referenceUtc.plusSeconds(2)),
                "nowUtc debe usar ZoneOffset.UTC, no la zona del JVM");
    }

    @Test
    void legacyUtcNaiveSeSerializaComoInstanteUtc() {
        LocalDateTime legacy = LocalDateTime.of(2026, 8, 31, 19, 0, 0);
        Instant instant = PedidoDateTimeSupport.toInstantUtc(legacy);
        assertEquals(Instant.parse("2026-08-31T19:00:00Z"), instant);
    }

    @Test
    void toInstantUtcNullRetornaNull() {
        assertNull(PedidoDateTimeSupport.toInstantUtc(null));
    }
}
