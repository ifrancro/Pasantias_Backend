package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.Usuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PedidoMapperSocioFieldsTest {

    @Test
    void mapperExponeNombreYTelefonoDelSocio() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Ruth");
        usuario.setApellido("Toro");
        usuario.setTelefono("+59173429001");

        Membresia membresia = new Membresia();
        membresia.setUsuario(usuario);
        membresia.setNumeroSocio("CL-000003");

        Pedido pedido = new Pedido();
        pedido.setMembresia(membresia);

        var dto = PedidoMapper.mapPedidoToPedidoDTO(pedido);

        assertEquals("Ruth Toro", dto.getSocioNombre());
        assertEquals("+59173429001", dto.getSocioTelefono());
        assertEquals("CL-000003", dto.getMembresiaNumeroSocio());
    }

    @Test
    void mapperMostradorSinMembresiaDejaSocioNull() {
        Pedido pedido = new Pedido();

        var dto = PedidoMapper.mapPedidoToPedidoDTO(pedido);

        assertNull(dto.getSocioNombre());
        assertNull(dto.getSocioTelefono());
        assertNull(dto.getMembresiaNumeroSocio());
    }

    @Test
    void resolverSocioNombreEvitaNullLiteral() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Ruth");
        usuario.setApellido(null);

        Membresia membresia = new Membresia();
        membresia.setUsuario(usuario);

        assertEquals("Ruth", PedidoMapper.resolverSocioNombre(membresia));
    }
}
