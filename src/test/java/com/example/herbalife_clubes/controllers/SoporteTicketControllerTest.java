package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.soporteticket.CrearSoporteTicketRequest;
import com.example.herbalife_clubes.dtos.soporteticket.SoporteTicketDTO;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.SoporteTicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoporteTicketControllerTest {

    @Mock
    private SoporteTicketService ticketService;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private SoporteTicketController controller;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTicketSinJwtDevuelve401() {
        ResponseEntity<SoporteTicketDTO> response =
                controller.createTicket(new CrearSoporteTicketRequest("Tipo", "Asunto", "Mensaje"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(ticketService, never()).createTicket(any(), any());
    }

    @Test
    void createTicketAutenticadoUsaUsuarioDelToken() {
        authenticateAs("socio@test.com");
        Usuario current = usuario(38, "SOCIO", "socio@test.com");
        when(usuarioRepository.findByEmail("socio@test.com")).thenReturn(Optional.of(current));
        SoporteTicketDTO dto = new SoporteTicketDTO();
        dto.setId(10);
        when(ticketService.createTicket(any(CrearSoporteTicketRequest.class), any())).thenReturn(dto);

        ResponseEntity<SoporteTicketDTO> response =
                controller.createTicket(new CrearSoporteTicketRequest("Tipo", "Asunto", "Mensaje"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        ArgumentCaptor<Integer> userCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(ticketService).createTicket(any(CrearSoporteTicketRequest.class), userCaptor.capture());
        assertEquals(38, userCaptor.getValue());
    }

    @Test
    void getMyTicketsDevuelveSoloPropios() {
        authenticateAs("host@test.com");
        Usuario current = usuario(20, "ANFITRION", "host@test.com");
        when(usuarioRepository.findByEmail("host@test.com")).thenReturn(Optional.of(current));
        when(ticketService.getMyTickets(20)).thenReturn(List.of(new SoporteTicketDTO()));

        ResponseEntity<List<SoporteTicketDTO>> response = controller.getMyTickets();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(ticketService).getMyTickets(20);
    }

    @Test
    void getTicketUsaUsuarioActualParaAutorizacion() {
        authenticateAs("socio@test.com");
        Usuario current = usuario(38, "SOCIO", "socio@test.com");
        when(usuarioRepository.findByEmail("socio@test.com")).thenReturn(Optional.of(current));
        SoporteTicketDTO dto = new SoporteTicketDTO();
        dto.setId(7);
        when(ticketService.getTicketAuthorized(7, current)).thenReturn(dto);

        ResponseEntity<SoporteTicketDTO> response = controller.getTicket(7);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(ticketService).getTicketAuthorized(7, current);
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "n/a"));
    }

    private static Usuario usuario(int id, String rolNombre, String email) {
        Rol rol = new Rol();
        rol.setNombre(rolNombre);
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(rol);
        u.setEmail(email);
        return u;
    }
}
