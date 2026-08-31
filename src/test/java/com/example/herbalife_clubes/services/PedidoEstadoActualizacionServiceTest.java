package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.serviceimpls.PedidoServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ORDER-STATUS-001: documenta la causa (mapper LAZY fuera de sesión con open-in-view=false)
 * y asegura que los métodos de mutación mantienen la transacción hasta devolver el DTO.
 */
class PedidoEstadoActualizacionServiceTest {

    @Test
    void actualizarEstadoDebeSerTransaccionalParaMaterializarRelacionesLazy() throws Exception {
        Transactional tx = PedidoServiceImpl.class
                .getMethod("actualizarEstado", Integer.class, String.class, Integer.class)
                .getAnnotation(Transactional.class);
        assertNotNull(tx, "actualizarEstado debe ser @Transactional: save() commitea en repo "
                + "y PedidoMapper accede a items/opciones/pedidoCombos LAZY");
        assertFalse(tx.readOnly());
    }

    @Test
    void cancelarPedidoDebeSerTransaccionalPorElMismoMotivo() throws Exception {
        Transactional tx = PedidoServiceImpl.class
                .getMethod("cancelarPedido", Integer.class)
                .getAnnotation(Transactional.class);
        assertNotNull(tx);
        assertFalse(tx.readOnly());
    }
}
