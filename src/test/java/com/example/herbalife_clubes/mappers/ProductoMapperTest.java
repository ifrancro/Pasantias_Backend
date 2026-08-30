package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

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
}
