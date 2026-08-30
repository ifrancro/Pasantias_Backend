package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.entities.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VentasDiariasComboReporteTest {

    @Test
    void reporteComboNoDobleCobraComponentes() throws Exception {
        VentasDiariasReporteServiceImpl service = new VentasDiariasReporteServiceImpl(null, null);

        PedidoCombo pedidoCombo = new PedidoCombo();
        pedidoCombo.setComboNombreSnapshot("Combo desayuno");
        pedidoCombo.setCantidad(1);
        pedidoCombo.setSubtotalSnapshot(bd("38"));

        PedidoItem batido = componente(7, "Batido", 1);
        PedidoItem te = componente(2, "Té", 1);
        batido.setPedidoCombo(pedidoCombo);
        te.setPedidoCombo(pedidoCombo);

        Pedido pedido = new Pedido();
        pedido.setEstado(EstadoPedido.ENTREGADO);
        pedido.setPedidoCombos(new ArrayList<>(List.of(pedidoCombo)));
        pedido.setItems(new ArrayList<>(List.of(batido, te)));

        Method m = VentasDiariasReporteServiceImpl.class.getDeclaredMethod(
                "construirFila", Pedido.class, LocalDate.class, int.class);
        m.setAccessible(true);
        var fila = (com.example.herbalife_clubes.dtos.reporte.RegistroVentaDiariaDTO)
                m.invoke(service, pedido, LocalDate.now(), 1);

        assertEquals(0, bd("38").compareTo(fila.getTotalBs()));
        assertEquals(1, fila.getProductos().size());
        assertEquals("Combo desayuno", fila.getProductos().get(0).getNombre());
        assertTrue(fila.getProductos().get(0).getEsCombo());
    }

    @Test
    void reporteMixtoComboMasSuelto() throws Exception {
        VentasDiariasReporteServiceImpl service = new VentasDiariasReporteServiceImpl(null, null);

        PedidoCombo pedidoCombo = new PedidoCombo();
        pedidoCombo.setComboNombreSnapshot("Combo");
        pedidoCombo.setCantidad(1);
        pedidoCombo.setSubtotalSnapshot(bd("38"));

        PedidoItem componente = componente(7, "Batido", 1);
        componente.setPedidoCombo(pedidoCombo);
        PedidoItem suelto = componente(10, "Extra", 1);
        suelto.setPrecioUnitario(bd("20"));
        suelto.setSubtotal(bd("20"));

        Pedido pedido = new Pedido();
        pedido.setPedidoCombos(new ArrayList<>(List.of(pedidoCombo)));
        pedido.setItems(new ArrayList<>(List.of(componente, suelto)));

        Method m = VentasDiariasReporteServiceImpl.class.getDeclaredMethod(
                "construirFila", Pedido.class, LocalDate.class, int.class);
        m.setAccessible(true);
        var fila = (com.example.herbalife_clubes.dtos.reporte.RegistroVentaDiariaDTO)
                m.invoke(service, pedido, LocalDate.now(), 1);

        assertEquals(0, bd("58").compareTo(fila.getTotalBs()));
        assertEquals(2, fila.getProductos().size());
    }

    private static PedidoItem componente(int id, String nombre, int cantidad) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre(nombre);
        PedidoItem item = new PedidoItem();
        item.setProducto(p);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(BigDecimal.ZERO);
        item.setSubtotal(BigDecimal.ZERO);
        return item;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
