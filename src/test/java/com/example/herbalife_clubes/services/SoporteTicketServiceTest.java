package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.soporteticket.CrearSoporteTicketRequest;
import com.example.herbalife_clubes.dtos.soporteticket.SoporteTicketDTO;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.SoporteTicket;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.SoporteTicketRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.SoporteTicketServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoporteTicketServiceTest {

    @Mock
    private SoporteTicketRepository ticketRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private SoporteTicketServiceImpl service;

    @Test
    void createTicketFuerzaPropietarioEstadoYSinRespuestaAdmin() {
        Usuario usuario = usuario(38, "SOCIO");
        when(usuarioRepository.findById(38)).thenReturn(Optional.of(usuario));
        when(ticketRepository.save(any(SoporteTicket.class))).thenAnswer(inv -> {
            SoporteTicket ticket = inv.getArgument(0);
            ticket.setId(1);
            return ticket;
        });

        CrearSoporteTicketRequest request = new CrearSoporteTicketRequest(
                "  Entrega  ", "  No llega mi pedido  ", "  Revisar por favor  ");

        SoporteTicketDTO dto = service.createTicket(request, 38);

        ArgumentCaptor<SoporteTicket> captor = ArgumentCaptor.forClass(SoporteTicket.class);
        verify(ticketRepository).save(captor.capture());
        SoporteTicket saved = captor.getValue();
        assertEquals(38, saved.getUsuario().getId());
        assertEquals("ABIERTO", saved.getEstado());
        assertNull(saved.getRespuestaAdmin());
        assertEquals("Entrega", saved.getTipoSolicitud());
        assertEquals("No llega mi pedido", saved.getAsunto());
        assertEquals("Revisar por favor", saved.getMensaje());
        assertEquals("ABIERTO", dto.getEstado());
    }

    @Test
    void createTicketConAsuntoBlankFalla() {
        Usuario usuario = usuario(38, "SOCIO");
        when(usuarioRepository.findById(38)).thenReturn(Optional.of(usuario));
        CrearSoporteTicketRequest request = new CrearSoporteTicketRequest("Tipo", "   ", "mensaje");
        assertThrows(IllegalArgumentException.class, () -> service.createTicket(request, 38));
    }

    @Test
    void getMyTicketsDevuelveSoloDelUsuario() {
        SoporteTicket t1 = ticket(1, 10, "ABIERTO");
        SoporteTicket t2 = ticket(2, 10, "RESUELTO");
        when(ticketRepository.findByUsuarioId(10)).thenReturn(List.of(t1, t2));

        List<SoporteTicketDTO> result = service.getMyTickets(10);
        assertEquals(2, result.size());
        assertEquals(10, result.get(0).getUsuarioId());
        assertEquals(10, result.get(1).getUsuarioId());
    }

    @Test
    void getTicketAuthorizedPermitePropietario() {
        SoporteTicket t = ticket(5, 38, "ABIERTO");
        when(ticketRepository.findById(5)).thenReturn(Optional.of(t));

        SoporteTicketDTO dto = service.getTicketAuthorized(5, usuario(38, "SOCIO"));
        assertEquals(5, dto.getId());
    }

    @Test
    void getTicketAuthorizedDeniegaUsuarioNoPropietario() {
        SoporteTicket t = ticket(5, 38, "ABIERTO");
        when(ticketRepository.findById(5)).thenReturn(Optional.of(t));
        assertThrows(AccessDeniedException.class,
                () -> service.getTicketAuthorized(5, usuario(77, "ANFITRION")));
    }

    @Test
    void getTicketAuthorizedPermiteAdmin() {
        SoporteTicket t = ticket(5, 38, "ABIERTO");
        when(ticketRepository.findById(5)).thenReturn(Optional.of(t));

        SoporteTicketDTO dto = service.getTicketAuthorized(5, usuario(1, "ADMIN"));
        assertEquals(5, dto.getId());
        assertEquals(38, dto.getUsuarioId());
    }

    private static SoporteTicket ticket(int id, int usuarioId, String estado) {
        SoporteTicket ticket = new SoporteTicket();
        ticket.setId(id);
        ticket.setUsuario(usuario(usuarioId, "SOCIO"));
        ticket.setTipoSolicitud("General");
        ticket.setAsunto("Asunto");
        ticket.setMensaje("Mensaje");
        ticket.setEstado(estado);
        return ticket;
    }

    private static Usuario usuario(int id, String rolNombre) {
        Rol rol = new Rol();
        rol.setNombre(rolNombre);
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        usuario.setNombre("User");
        usuario.setApellido("Test");
        return usuario;
    }
}
