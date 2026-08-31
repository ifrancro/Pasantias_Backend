package com.example.herbalife_clubes.pedidos;

import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.PedidoCombo;
import com.example.herbalife_clubes.entities.PedidoItem;
import com.example.herbalife_clubes.entities.Producto;

/**
 * Cálculo de puntos por compras (POINTS-ORDER-001).
 * Combos modernos: solo {@code PedidoCombo.puntosValorSnapshot × cantidad}.
 * Items con {@code pedidoCombo != null} son componentes y no suman puntos extra.
 */
public final class PedidoPuntosSupport {

    private PedidoPuntosSupport() {
    }

    public static int calcularPuntosGanados(Pedido pedido) {
        int total = 0;
        if (pedido.getPedidoCombos() != null) {
            for (PedidoCombo pedidoCombo : pedido.getPedidoCombos()) {
                total += puntosCombo(pedidoCombo);
            }
        }
        if (pedido.getItems() != null) {
            for (PedidoItem item : pedido.getItems()) {
                if (item.getPedidoCombo() != null) {
                    continue;
                }
                total += puntosItemSuelto(item);
            }
        }
        return total;
    }

    /**
     * Usa snapshot persistido si existe; si es null (pedido histórico), calcula fallback una sola vez
     * y congela el valor en el pedido antes de acreditar.
     */
    public static int resolverPuntosParaAcreditacion(Pedido pedido) {
        if (pedido.getPuntosGanados() != null) {
            return pedido.getPuntosGanados();
        }
        int calculado = calcularPuntosGanados(pedido);
        pedido.setPuntosGanados(calculado);
        return calculado;
    }

    private static int puntosCombo(PedidoCombo pedidoCombo) {
        int puntos = pedidoCombo.getPuntosValorSnapshot() != null ? pedidoCombo.getPuntosValorSnapshot() : 0;
        int cantidad = pedidoCombo.getCantidad() != null ? pedidoCombo.getCantidad() : 0;
        return puntos * cantidad;
    }

    private static int puntosItemSuelto(PedidoItem item) {
        Producto producto = item.getProducto();
        int puntos = producto != null && producto.getPuntosValor() != null ? producto.getPuntosValor() : 0;
        int cantidad = item.getCantidad() != null ? item.getCantidad() : 0;
        return puntos * cantidad;
    }
}
