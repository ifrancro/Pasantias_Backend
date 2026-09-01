package com.example.herbalife_clubes.pedidos;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Semántica temporal de {@code pedidos.fecha_pedido}: columna naive que almacena UTC.
 */
public final class PedidoDateTimeSupport {

    private PedidoDateTimeSupport() {
    }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /** Convierte el valor UTC-naive persistido a un instante inequívoco para la API. */
    public static Instant toInstantUtc(LocalDateTime utcNaive) {
        if (utcNaive == null) {
            return null;
        }
        return utcNaive.toInstant(ZoneOffset.UTC);
    }
}
