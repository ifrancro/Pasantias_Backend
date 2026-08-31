package com.example.herbalife_clubes.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SoporteTicketControllerSecurityAnnotationsTest {

    @Test
    void createTicketEsParaCualquierAutenticado() throws Exception {
        PreAuthorize pa = SoporteTicketController.class
                .getMethod("createTicket", com.example.herbalife_clubes.dtos.soporteticket.CrearSoporteTicketRequest.class)
                .getAnnotation(PreAuthorize.class);
        assertNotNull(pa);
        assertEquals("isAuthenticated()", pa.value());
    }

    @Test
    void miosEsParaCualquierAutenticado() throws Exception {
        PreAuthorize pa = SoporteTicketController.class
                .getMethod("getMyTickets")
                .getAnnotation(PreAuthorize.class);
        assertNotNull(pa);
        assertEquals("isAuthenticated()", pa.value());
    }

    @Test
    void usuarioEsSoloAdmin() throws Exception {
        PreAuthorize pa = SoporteTicketController.class
                .getMethod("getTicketsByUsuario", Integer.class)
                .getAnnotation(PreAuthorize.class);
        assertNotNull(pa);
        assertEquals("hasRole('ADMIN')", pa.value());
    }

    @Test
    void adminEndpointsSeMantienenSoloAdmin() throws Exception {
        PreAuthorize listar = SoporteTicketController.class
                .getMethod("getAllTickets")
                .getAnnotation(PreAuthorize.class);
        PreAuthorize responder = SoporteTicketController.class
                .getMethod("responderTicket", Integer.class,
                        com.example.herbalife_clubes.dtos.soporteticket.ResponderTicketRequest.class)
                .getAnnotation(PreAuthorize.class);
        PreAuthorize estado = SoporteTicketController.class
                .getMethod("cambiarEstado", Integer.class, String.class)
                .getAnnotation(PreAuthorize.class);

        assertNotNull(listar);
        assertNotNull(responder);
        assertNotNull(estado);
        assertEquals("hasRole('ADMIN')", listar.value());
        assertEquals("hasRole('ADMIN')", responder.value());
        assertEquals("hasRole('ADMIN')", estado.value());
    }
}
