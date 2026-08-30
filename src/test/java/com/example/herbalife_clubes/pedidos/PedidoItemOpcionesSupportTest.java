package com.example.herbalife_clubes.pedidos;

import com.example.herbalife_clubes.dtos.pedido.PedidoItemOpcionResponseDTO;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.ProductoGrupoOpcion;
import com.example.herbalife_clubes.entities.ProductoOpcion;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PedidoItemOpcionesSupportTest {

    @Test
    void productoSinGruposAceptaOpcionesVacias() {
        Producto producto = productoBase();
        assertTrue(PedidoItemOpcionesSupport.validarYMaterializar(producto, List.of()).isEmpty());
        assertTrue(PedidoItemOpcionesSupport.validarYMaterializar(producto, null).isEmpty());
    }

    @Test
    void productoSinGruposRechazaOpcionesEnPayload() {
        Producto producto = productoBase();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PedidoItemOpcionesSupport.validarYMaterializar(producto,
                        List.of(sel(3, 6, 1))));
        assertTrue(ex.getMessage().contains("no tiene grupos"));
    }

    @Test
    void minMaxRepeatPermitidos() {
        Producto producto = productoConSaboresYConsistencia();
        var result = PedidoItemOpcionesSupport.validarYMaterializar(producto, List.of(
                sel(3, 6, 2),
                sel(4, 9, 1)));
        assertEquals(2, result.size());
        assertEquals("Frutilla", result.get(0).getOpcionNombreSnapshot());
        assertEquals("Cremoso", result.get(1).getOpcionNombreSnapshot());
        assertEquals(2, result.get(0).getCantidad());
    }

    @Test
    void maxExcedidoRechaza400() {
        Producto producto = productoConSaboresYConsistencia();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PedidoItemOpcionesSupport.validarYMaterializar(producto, List.of(
                        sel(3, 6, 2),
                        sel(3, 7, 2))));
        assertTrue(ex.getMessage().contains("como máximo"));
    }

    @Test
    void minNoCumplidoRechaza400() {
        Producto producto = productoConSaboresYConsistencia();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PedidoItemOpcionesSupport.validarYMaterializar(producto, List.of(
                        sel(4, 9, 1))));
        assertTrue(ex.getMessage().contains("al menos"));
    }

    @Test
    void repeatFalseCantidadDosRechaza400() {
        Producto producto = productoConSaboresYConsistencia();
        producto.getGruposOpciones().get(1).setMaxSelecciones(2);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PedidoItemOpcionesSupport.validarYMaterializar(producto, List.of(
                        sel(3, 6, 1),
                        sel(4, 9, 2))));
        assertTrue(ex.getMessage().contains("no puede repetirse"));
    }

    @Test
    void opcionInactivaRechaza400() {
        Producto producto = productoConSaboresYConsistencia();
        opcion(producto, 3, 7).setActivo(false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PedidoItemOpcionesSupport.validarYMaterializar(producto, List.of(
                        sel(3, 6, 1),
                        sel(3, 7, 1),
                        sel(4, 9, 1))));
        assertTrue(ex.getMessage().contains("ya no está disponible"));
    }

    @Test
    void opcionIdRepetidoRechaza400() {
        Producto producto = productoConSaboresYConsistencia();
        assertThrows(IllegalArgumentException.class,
                () -> PedidoItemOpcionesSupport.validarYMaterializar(producto, List.of(
                        sel(3, 6, 1),
                        sel(3, 6, 1),
                        sel(4, 9, 1))));
    }

    @Test
    void opcionDeOtroGrupoRechaza400() {
        Producto producto = productoConSaboresYConsistencia();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PedidoItemOpcionesSupport.validarYMaterializar(producto, List.of(
                        sel(3, 9, 1),
                        sel(4, 9, 1))));
        assertTrue(ex.getMessage().contains("grupo indicado"));
    }

    @Test
    void snapshotUsaNombresDelServidorNoDelCliente() {
        Producto producto = productoConSaboresYConsistencia();
        PedidoItemOpcionResponseDTO sel = sel(3, 6, 1);
        sel.setOpcionNombre("Nombre Falsificado");
        sel.setGrupoNombre("Grupo Falso");

        var result = PedidoItemOpcionesSupport.validarYMaterializar(producto, List.of(
                sel,
                sel(4, 9, 1)));

        assertEquals("Frutilla", result.get(0).getOpcionNombreSnapshot());
        assertEquals("Sabores", result.get(0).getGrupoNombreSnapshot());
    }

    @Test
    void grupoRequeridoSinOpcionesActivasSuficientesRechaza400() {
        Producto producto = productoConSaboresYConsistencia();
        producto.getGruposOpciones().get(0).getOpciones().forEach(o -> o.setActivo(false));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PedidoItemOpcionesSupport.validarYMaterializar(producto, List.of()));
        assertTrue(ex.getMessage().contains("opciones disponibles"));
    }

    private static Producto productoBase() {
        Producto p = new Producto();
        p.setId(7);
        p.setNombre("Batido");
        p.setGruposOpciones(new ArrayList<>());
        return p;
    }

    private static Producto productoConSaboresYConsistencia() {
        Producto producto = productoBase();
        ProductoGrupoOpcion sabores = grupo(producto, 3, "Sabores", 0, 1, 2, true);
        sabores.getOpciones().add(opcion(sabores, 6, "Frutilla", 0));
        sabores.getOpciones().add(opcion(sabores, 7, "Cookies", 1));
        ProductoGrupoOpcion consistencia = grupo(producto, 4, "Consistencia", 1, 1, 1, false);
        consistencia.getOpciones().add(opcion(consistencia, 9, "Cremoso", 0));
        consistencia.getOpciones().add(opcion(consistencia, 10, "Líquido", 1));
        producto.getGruposOpciones().add(sabores);
        producto.getGruposOpciones().add(consistencia);
        return producto;
    }

    private static ProductoGrupoOpcion grupo(
            Producto producto, int id, String nombre, int orden, int min, Integer max, boolean repeat) {
        ProductoGrupoOpcion g = new ProductoGrupoOpcion();
        g.setId(id);
        g.setProducto(producto);
        g.setNombre(nombre);
        g.setOrden(orden);
        g.setMinSelecciones(min);
        g.setMaxSelecciones(max);
        g.setPermiteRepetir(repeat);
        g.setOpciones(new ArrayList<>());
        return g;
    }

    private static ProductoOpcion opcion(ProductoGrupoOpcion grupo, int id, String nombre, int orden) {
        ProductoOpcion o = new ProductoOpcion();
        o.setId(id);
        o.setGrupo(grupo);
        o.setNombre(nombre);
        o.setOrden(orden);
        o.setActivo(true);
        return o;
    }

    private static ProductoOpcion opcion(Producto producto, int grupoId, int opcionId) {
        return producto.getGruposOpciones().stream()
                .filter(g -> g.getId() == grupoId)
                .flatMap(g -> g.getOpciones().stream())
                .filter(o -> o.getId() == opcionId)
                .findFirst()
                .orElseThrow();
    }

    private static PedidoItemOpcionResponseDTO sel(int grupoId, int opcionId, int cantidad) {
        PedidoItemOpcionResponseDTO dto = new PedidoItemOpcionResponseDTO();
        dto.setGrupoId(grupoId);
        dto.setOpcionId(opcionId);
        dto.setCantidad(cantidad);
        return dto;
    }
}
