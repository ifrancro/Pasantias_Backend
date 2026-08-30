package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.entities.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PedidoMapperComboTest {

    @Test
    void totalMixtoComboMasProductoSuelto() {
        Pedido pedido = new Pedido();

        PedidoCombo pedidoCombo = new PedidoCombo();
        pedidoCombo.setId(15);
        pedidoCombo.setComboNombreSnapshot("Combo desayuno");
        pedidoCombo.setCantidad(1);
        pedidoCombo.setPrecioUnitarioSnapshot(bd("38"));
        pedidoCombo.setSubtotalSnapshot(bd("38"));
        pedidoCombo.setPuntosValorSnapshot(15);

        PedidoItem componente = item(7, 1, bd("0"), bd("0"));
        componente.setPedidoCombo(pedidoCombo);

        PedidoItem suelto = item(10, 1, bd("20"), bd("20"));

        pedido.setPedidoCombos(new ArrayList<>(List.of(pedidoCombo)));
        pedido.setItems(new ArrayList<>(List.of(componente, suelto)));

        assertEquals(0, bd("58").compareTo(PedidoMapper.calcularTotalPedido(pedido)));

        var dto = PedidoMapper.mapPedidoToPedidoDTO(pedido);
        assertEquals(1, dto.getCombos().size());
        assertEquals(0, bd("38").compareTo(dto.getCombos().get(0).getSubtotal()));
        assertEquals(0, bd("58").compareTo(dto.getTotal()));
        assertEquals(2, dto.getItems().size());
        assertNotNull(dto.getItems().get(0).getPedidoComboId());
    }

    @Test
    void legacyPedidoSinPedidoComboUsaComboIdEnItem() {
        Combo combo = new Combo();
        combo.setId(4);
        combo.setNombre("Combo legacy");

        PedidoItem legacy = item(7, 1, bd("20"), bd("20"));
        legacy.setCombo(combo);

        Pedido pedido = new Pedido();
        pedido.setItems(new ArrayList<>(List.of(legacy)));

        var dto = PedidoMapper.mapPedidoToPedidoDTO(pedido);
        assertTrue(dto.getCombos().isEmpty());
        assertEquals(4, dto.getItems().get(0).getComboId());
        assertEquals("Combo legacy", dto.getItems().get(0).getComboNombre());
    }

    private static PedidoItem item(int productoId, int cantidad, BigDecimal unit, BigDecimal subtotal) {
        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setNombre("P" + productoId);

        PedidoItem item = new PedidoItem();
        item.setProducto(producto);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(unit);
        item.setSubtotal(subtotal);
        return item;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
