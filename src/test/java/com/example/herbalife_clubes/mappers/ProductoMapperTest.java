package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
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
    void mapeoPublicoOcultaRevisionEIngredientes() throws Exception {
        ProductoDTO dto = ProductoMapper.mapProductoToProductoDTO(productoConRevision(), false);

        assertNull(dto.getComentarioRevision());
        assertNull(dto.getRevisadoPorUsuarioId());
        assertNull(dto.getRevisadoPorNombre());
        assertNull(dto.getRevisadoAt());
        assertNull(dto.getIngredientes());

        ObjectMapper json = new ObjectMapper().findAndRegisterModules();
        String body = json.writeValueAsString(dto);
        assertFalse(body.contains("comentarioRevision"));
        assertFalse(body.contains("revisadoPorUsuarioId"));
        assertFalse(body.contains("revisadoPorNombre"));
        assertFalse(body.contains("revisadoAt"));
        assertFalse(body.contains("gruposOpciones"));
        assertNull(dto.getGruposOpciones());
    }

    @Test
    void mapeoInternoExponeDosGruposYOpcionesEnOrden() {
        Producto producto = productoConRevision();
        producto.setGruposOpciones(List.of(
                grupo(1, "Sabores", 0, List.of(
                        opcion(10, "Frutilla", 0),
                        opcion(11, "Vainilla", 1))),
                grupo(2, "Consistencia", 1, List.of(
                        opcion(20, "Cremoso", 0),
                        opcion(21, "Líquido", 1)))));

        ProductoDTO dto = ProductoMapper.mapProductoToProductoDTO(producto, true);

        assertEquals(2, dto.getGruposOpciones().size());
        assertEquals(List.of("Sabores", "Consistencia"),
                dto.getGruposOpciones().stream().map(g -> g.getNombre()).toList());
        assertEquals(List.of("Frutilla", "Vainilla"),
                dto.getGruposOpciones().get(0).getOpciones().stream().map(o -> o.getNombre()).toList());
        assertEquals(List.of("Cremoso", "Líquido"),
                dto.getGruposOpciones().get(1).getOpciones().stream().map(o -> o.getNombre()).toList());
    }

    static Producto productoConRevision() {
        Hub hub = new Hub();
        hub.setId(1);
        hub.setNombre("HUB");

        Usuario admin = new Usuario();
        admin.setId(7);
        admin.setNombre("Ana");
        admin.setApellido("Admin");

        Producto producto = new Producto();
        producto.setId(3);
        producto.setHub(hub);
        producto.setNombre("Batido");
        producto.setIngredientes("leche, cacao");
        producto.setEstadoAprobacion("RECHAZADO");
        producto.setComentarioRevision("Faltan ingredientes");
        producto.setRevisadoPor(admin);
        producto.setRevisadoAt(LocalDateTime.of(2026, 8, 29, 12, 0));
        return producto;
    }

    private static ProductoGrupoOpcion grupo(
            int id, String nombre, int orden, List<ProductoOpcion> opciones) {
        ProductoGrupoOpcion grupo = new ProductoGrupoOpcion();
        grupo.setId(id);
        grupo.setNombre(nombre);
        grupo.setOrden(orden);
        grupo.setMinSelecciones(1);
        grupo.setOpciones(opciones);
        return grupo;
    }

    private static ProductoOpcion opcion(int id, String nombre, int orden) {
        ProductoOpcion opcion = new ProductoOpcion();
        opcion.setId(id);
        opcion.setNombre(nombre);
        opcion.setOrden(orden);
        opcion.setActivo(true);
        return opcion;
    }
}
