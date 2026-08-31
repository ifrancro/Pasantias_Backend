package com.example.herbalife_clubes.pedidos;

import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.PedidoCombo;
import com.example.herbalife_clubes.entities.PedidoItem;
import com.example.herbalife_clubes.entities.Producto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PedidoPuntosSupportTest {

    @Test
    void productoSueltoMultiplicaPuntosPorCantidad() {
        Pedido pedido = new Pedido();
        pedido.setItems(List.of(item(10, 2, null)));

        assertEquals(20, PedidoPuntosSupport.calcularPuntosGanados(pedido));
    }

    @Test
    void productoPuntosNullCuentaComoCero() {
        Pedido pedido = new Pedido();
        pedido.setItems(List.of(item(null, 3, null)));

        assertEquals(0, PedidoPuntosSupport.calcularPuntosGanados(pedido));
    }

    @Test
    void comboSnapshotMultiplicaPorCantidadCombo() {
        Pedido pedido = new Pedido();
        PedidoCombo combo = comboLine(15, 2);
        pedido.setPedidoCombos(List.of(combo));
        pedido.setItems(List.of(
                item(5, 1, combo),
                item(3, 1, combo)));

        assertEquals(30, PedidoPuntosSupport.calcularPuntosGanados(pedido));
    }

    @Test
    void pedidoMixtoSumaComboYSuelto() {
        Pedido pedido = new Pedido();
        PedidoCombo combo = comboLine(15, 1);
        pedido.setPedidoCombos(List.of(combo));
        pedido.setItems(new ArrayList<>(List.of(
                item(5, 1, combo),
                item(10, 2, null))));

        assertEquals(35, PedidoPuntosSupport.calcularPuntosGanados(pedido));
    }

    @Test
    void resolverPuntosUsaSnapshotSiExiste() {
        Pedido pedido = new Pedido();
        pedido.setPuntosGanados(42);
        pedido.setItems(List.of(item(100, 5, null)));

        assertEquals(42, PedidoPuntosSupport.resolverPuntosParaAcreditacion(pedido));
        assertEquals(42, pedido.getPuntosGanados());
    }

    @Test
    void resolverPuntosFallbackCongelaEnPedido() {
        Pedido pedido = new Pedido();
        pedido.setItems(List.of(item(7, 2, null)));

        assertEquals(14, PedidoPuntosSupport.resolverPuntosParaAcreditacion(pedido));
        assertEquals(14, pedido.getPuntosGanados());
    }

    private static PedidoCombo comboLine(int puntosSnapshot, int cantidad) {
        PedidoCombo pc = new PedidoCombo();
        pc.setPuntosValorSnapshot(puntosSnapshot);
        pc.setCantidad(cantidad);
        return pc;
    }

    private static PedidoItem item(Integer puntosValor, int cantidad, PedidoCombo pedidoCombo) {
        Producto producto = new Producto();
        producto.setPuntosValor(puntosValor);
        PedidoItem item = new PedidoItem();
        item.setProducto(producto);
        item.setCantidad(cantidad);
        item.setPedidoCombo(pedidoCombo);
        return item;
    }
}
