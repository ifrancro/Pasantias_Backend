package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoGrupoOpcionDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoOpcionDTO;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.ProductoGrupoOpcion;
import com.example.herbalife_clubes.entities.ProductoOpcion;
import com.example.herbalife_clubes.entities.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductoMapperTest {

    @Test
    void mapeoInternoExponeRevision() {
        ProductoDTO dto = ProductoMapper.mapProductoToProductoDTO(productoConRevision(), true);

        assertEquals("Faltan ingredientes", dto.getComentarioRevision());
        assertEquals(7, dto.getRevisadoPorUsuarioId());
        assertEquals("Ana Admin", dto.getRevisadoPorNombre());
        assertNotNull(dto.getRevisadoAt());
        assertEquals("leche, cacao", dto.getIngredientes());
        assertNotNull(dto.getGruposOpciones());
        assertTrue(dto.getGruposOpciones().isEmpty());
    }

    @Test
    void mapeoPublicoOcultaRevisionEIngredientesYExponeGruposVacios() throws Exception {
        ProductoDTO dto = ProductoMapper.mapProductoToProductoDTO(productoConRevision(), false);

        assertNull(dto.getComentarioRevision());
        assertNull(dto.getRevisadoPorUsuarioId());
        assertNull(dto.getRevisadoPorNombre());
        assertNull(dto.getRevisadoAt());
        assertNull(dto.getIngredientes());
        assertNotNull(dto.getGruposOpciones());
        assertTrue(dto.getGruposOpciones().isEmpty());

        ObjectMapper json = new ObjectMapper().findAndRegisterModules();
        String body = json.writeValueAsString(dto);
        assertFalse(body.contains("comentarioRevision"));
        assertFalse(body.contains("revisadoPorUsuarioId"));
        assertFalse(body.contains("revisadoPorNombre"));
        assertFalse(body.contains("revisadoAt"));
        assertTrue(body.contains("gruposOpciones"));
    }

    @Test
    void mapeoInternoExponeDosGruposYOpcionesEnOrden() {
        Producto producto = productoConRevision();
        producto.setGruposOpciones(List.of(
                grupo(1, "Sabores", 0, List.of(
                        opcion(10, "Frutilla", 0, true),
                        opcion(11, "Vainilla", 1, true))),
                grupo(2, "Consistencia", 1, List.of(
                        opcion(20, "Cremoso", 0, true),
                        opcion(21, "Líquido", 1, true)))));

        ProductoDTO dto = ProductoMapper.mapProductoToProductoDTO(producto, true);

        assertEquals(2, dto.getGruposOpciones().size());
        assertEquals(List.of("Sabores", "Consistencia"),
                dto.getGruposOpciones().stream().map(ProductoGrupoOpcionDTO::getNombre).toList());
        assertEquals(List.of("Frutilla", "Vainilla"),
                dto.getGruposOpciones().get(0).getOpciones().stream().map(ProductoOpcionDTO::getNombre).toList());
        assertEquals(List.of("Cremoso", "Líquido"),
                dto.getGruposOpciones().get(1).getOpciones().stream().map(ProductoOpcionDTO::getNombre).toList());
    }

    @Test
    void mapeoPublicoExponeGruposYFiltraOpcionesInactivas() {
        Producto producto = productoAprobado();
        producto.setGruposOpciones(List.of(
                grupo(2, "Sabores", 0, 1, 1, true, List.of(
                        opcion(3, "frutilla", 0, true),
                        opcion(4, "Cookies", 1, true),
                        opcion(5, "Oculto", 2, false)))));

        ProductoDTO dto = ProductoMapper.mapProductoToProductoDTO(producto, ProductoMapper.Vista.PUBLICO);

        assertNull(dto.getIngredientes());
        assertNull(dto.getComentarioRevision());
        assertEquals(1, dto.getGruposOpciones().size());
        ProductoGrupoOpcionDTO sabores = dto.getGruposOpciones().get(0);
        assertEquals(2, sabores.getId());
        assertEquals("Sabores", sabores.getNombre());
        assertEquals(0, sabores.getOrden());
        assertEquals(1, sabores.getMinSelecciones());
        assertEquals(1, sabores.getMaxSelecciones());
        assertTrue(sabores.getPermiteRepetir());
        assertEquals(List.of("frutilla", "Cookies"),
                sabores.getOpciones().stream().map(ProductoOpcionDTO::getNombre).toList());
        assertEquals(List.of(3, 4),
                sabores.getOpciones().stream().map(ProductoOpcionDTO::getId).toList());
        assertTrue(sabores.getOpciones().stream().allMatch(ProductoOpcionDTO::getActivo));
    }

    @Test
    void mapeoInternoIncluyeOpcionesInactivas() {
        Producto producto = productoAprobado();
        producto.setGruposOpciones(List.of(
                grupo(2, "Sabores", 0, List.of(
                        opcion(3, "frutilla", 0, true),
                        opcion(5, "Oculto", 1, false)))));

        ProductoDTO dto = ProductoMapper.mapProductoToProductoDTO(producto, ProductoMapper.Vista.INTERNO);

        assertEquals(List.of("frutilla", "Oculto"),
                dto.getGruposOpciones().get(0).getOpciones().stream().map(ProductoOpcionDTO::getNombre).toList());
        assertFalse(dto.getGruposOpciones().get(0).getOpciones().get(1).getActivo());
        assertEquals("leche, cacao", dto.getIngredientes());
        assertEquals("Faltan ingredientes", dto.getComentarioRevision());
    }

    @Test
    void mapeoPublicoMantieneGrupoSinOpcionesActivas() {
        Producto producto = productoAprobado();
        producto.setGruposOpciones(List.of(
                grupo(2, "Sabores", 0, 1, 1, true, List.of(
                        opcion(5, "Oculto", 0, false),
                        opcion(6, "También oculto", 1, false)))));

        ProductoDTO dto = ProductoMapper.mapProductoToProductoDTO(producto, ProductoMapper.Vista.PUBLICO);

        assertEquals(1, dto.getGruposOpciones().size());
        ProductoGrupoOpcionDTO sabores = dto.getGruposOpciones().get(0);
        assertEquals("Sabores", sabores.getNombre());
        assertEquals(1, sabores.getMinSelecciones());
        assertNotNull(sabores.getOpciones());
        assertTrue(sabores.getOpciones().isEmpty());
    }

    @Test
    void mapeoPublicoProductoViejoSinGruposDevuelveListaVacia() {
        ProductoDTO dto = ProductoMapper.mapProductoToProductoDTO(productoAprobado(), ProductoMapper.Vista.PUBLICO);

        assertNotNull(dto.getGruposOpciones());
        assertTrue(dto.getGruposOpciones().isEmpty());
    }

    @Test
    void mapeoPublicoConservaOrdenTrasFiltrarInactivas() {
        Producto producto = productoAprobado();
        producto.setGruposOpciones(List.of(
                grupo(2, "B", 1, List.of(opcion(20, "Z", 1, true), opcion(21, "oculto", 0, false))),
                grupo(1, "A", 0, List.of(
                        opcion(11, "Y", 2, true),
                        opcion(12, "oculto", 1, false),
                        opcion(10, "X", 0, true)))));

        ProductoDTO dto = ProductoMapper.mapProductoToProductoDTO(producto, ProductoMapper.Vista.PUBLICO);

        assertEquals(List.of("A", "B"),
                dto.getGruposOpciones().stream().map(ProductoGrupoOpcionDTO::getNombre).toList());
        assertEquals(List.of("X", "Y"),
                dto.getGruposOpciones().get(0).getOpciones().stream().map(ProductoOpcionDTO::getNombre).toList());
        assertEquals(List.of("Z"),
                dto.getGruposOpciones().get(1).getOpciones().stream().map(ProductoOpcionDTO::getNombre).toList());
    }

    static Producto productoConRevision() {
        return productoConEstado("RECHAZADO");
    }

    static Producto productoAprobado() {
        Producto producto = productoConEstado("APROBADO");
        producto.setActivo(true);
        return producto;
    }

    static Producto productoConEstado(String estado) {
        Hub hub = new Hub();
        hub.setId(1);
        hub.setNombre("HUB");

        Usuario admin = new Usuario();
        admin.setId(7);
        admin.setNombre("Ana");
        admin.setApellido("Admin");

        Producto producto = new Producto();
        producto.setId(7);
        producto.setHub(hub);
        producto.setNombre("Batido de leche");
        producto.setIngredientes("leche, cacao");
        producto.setEstadoAprobacion(estado);
        producto.setActivo(true);
        producto.setComentarioRevision("Faltan ingredientes");
        producto.setRevisadoPor(admin);
        producto.setRevisadoAt(LocalDateTime.of(2026, 8, 29, 12, 0));
        return producto;
    }

    private static ProductoGrupoOpcion grupo(
            int id, String nombre, int orden, List<ProductoOpcion> opciones) {
        return grupo(id, nombre, orden, 1, null, false, opciones);
    }

    private static ProductoGrupoOpcion grupo(
            int id, String nombre, int orden, int min, Integer max, boolean repetir,
            List<ProductoOpcion> opciones) {
        ProductoGrupoOpcion grupo = new ProductoGrupoOpcion();
        grupo.setId(id);
        grupo.setNombre(nombre);
        grupo.setOrden(orden);
        grupo.setMinSelecciones(min);
        grupo.setMaxSelecciones(max);
        grupo.setPermiteRepetir(repetir);
        grupo.setOpciones(opciones);
        return grupo;
    }

    private static ProductoOpcion opcion(int id, String nombre, int orden, boolean activo) {
        ProductoOpcion opcion = new ProductoOpcion();
        opcion.setId(id);
        opcion.setNombre(nombre);
        opcion.setOrden(orden);
        opcion.setActivo(activo);
        return opcion;
    }
}
