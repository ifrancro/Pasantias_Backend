package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.pedido.PedidoConItemsDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoMostradorRequestDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.pedidos.PedidoComboSupport;
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.ComboRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.NotificacionRepository;
import com.example.herbalife_clubes.repositories.PedidoRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.PedidoServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceFechaPedidoTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private MembresiaRepository membresiaRepository;
    @Mock
    private ClubRepository clubRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ClubProductoRepository clubProductoRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ComboRepository comboRepository;
    @Mock
    private PedidoComboSupport pedidoComboSupport;

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPedidoConItemsPersisteFechaUtcYExponeInstant() {
        stubSocioClub();
        Producto producto = producto(10, "Batido", bd("25.00"));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10))
                .thenReturn(Optional.of(cp(producto, true, bd("28.00"))));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido pedido = inv.getArgument(0);
            pedido.setId(100);
            return pedido;
        });

        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setItems(List.of(item(10, 1, null)));

        PedidoDTO creado = pedidoService.createPedidoConItems(request, 1, 3);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());

        LocalDateTime fechaPersistida = captor.getAllValues().get(0).getFechaPedido();
        assertNotNull(fechaPersistida);
        LocalDateTime reference = LocalDateTime.now(ZoneOffset.UTC);
        assertTrue(
                fechaPersistida.isAfter(reference.minusSeconds(2))
                        && fechaPersistida.isBefore(reference.plusSeconds(2)));

        assertNotNull(creado.getFechaPedido());
        assertEquals(
                fechaPersistida.toInstant(ZoneOffset.UTC),
                creado.getFechaPedido());
    }

    @Test
    void createPedidoMostradorPersisteFechaUtcYExponeInstant() {
        authenticateAs("host@club.com");
        Usuario host = anfitrion(20);
        Club club = clubActivo(host);
        Producto producto = producto(10, "Shake", bd("25.00"));
        producto.setTipo("GLOBAL");
        producto.setEstadoAprobacion("APROBADO");
        producto.setActivo(true);
        producto.setHub(club.getHub());

        when(usuarioRepository.findByEmail("host@club.com")).thenReturn(Optional.of(host));
        when(clubRepository.findByIdAndAnfitrionId(3, 20)).thenReturn(Optional.of(club));
        when(productoRepository.findByHubIdAndTipoAndEstadoAprobacion(1, "GLOBAL", "APROBADO"))
                .thenReturn(List.of(producto));
        when(productoRepository.findByClubCreadorIdAndTipoAndEstadoAprobacion(3, "LOCAL", "APROBADO"))
                .thenReturn(List.of());
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10))
                .thenReturn(Optional.of(cp(producto, true, bd("28.00"))));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido pedido = inv.getArgument(0);
            pedido.setId(101);
            return pedido;
        });

        PedidoMostradorRequestDTO request = new PedidoMostradorRequestDTO();
        request.setClubId(3);
        request.setTipoPago("EFECTIVO");
        request.setItems(List.of(item(10, 1, null)));

        PedidoDTO creado = pedidoService.createPedidoMostrador(request);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());

        LocalDateTime fechaPersistida = captor.getAllValues().get(0).getFechaPedido();
        assertNotNull(fechaPersistida);
        LocalDateTime reference = LocalDateTime.now(ZoneOffset.UTC);
        assertTrue(
                fechaPersistida.isAfter(reference.minusSeconds(2))
                        && fechaPersistida.isBefore(reference.plusSeconds(2)));

        assertNotNull(creado.getFechaPedido());
        assertEquals(Instant.parse(fechaPersistida.atZone(ZoneOffset.UTC).toInstant().toString()),
                creado.getFechaPedido());
    }

    private void stubSocioClub() {
        Usuario host = anfitrion(20);
        Club club = clubActivo(host);
        Usuario socio = socio(5, "socio@test.com");
        Membresia membresia = new Membresia();
        membresia.setId(1);
        membresia.setEstado("ACTIVA");
        membresia.setNumeroSocio("SC-1");
        membresia.setClub(club);
        membresia.setUsuario(socio);
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresia));
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(usuarioRepository.findByEmail("socio@test.com")).thenReturn(Optional.of(socio));
        authenticateAs("socio@test.com");
    }

    private static PedidoItemDTO item(int productoId, int cantidad, BigDecimal precioMentira) {
        PedidoItemDTO dto = new PedidoItemDTO();
        dto.setProductoId(productoId);
        dto.setCantidad(cantidad);
        dto.setPrecioUnitario(precioMentira);
        return dto;
    }

    private static ClubProducto cp(Producto producto, boolean disponible, BigDecimal precioVenta) {
        ClubProducto cp = new ClubProducto();
        cp.setProducto(producto);
        cp.setDisponible(disponible);
        cp.setPrecioVenta(precioVenta);
        return cp;
    }

    private static Producto producto(int id, String nombre, BigDecimal precio) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setPuntosValor(0);
        producto.setEstadoAprobacion("APROBADO");
        producto.setActivo(true);
        return producto;
    }

    private static Usuario anfitrion(int id) {
        Rol rol = new Rol();
        rol.setNombre("ANFITRION");
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        usuario.setEmail("host@club.com");
        return usuario;
    }

    private static Usuario socio(int id, String email) {
        Rol rol = new Rol();
        rol.setNombre("SOCIO");
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        usuario.setEmail(email);
        return usuario;
    }

    private static Club clubActivo(Usuario host) {
        Hub hub = new Hub();
        hub.setId(1);
        Club club = new Club();
        club.setId(3);
        club.setEstado("ACTIVO");
        club.setNombreClub("Club 3");
        club.setHub(hub);
        club.setAnfitrion(host);
        return club;
    }

    private static void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "n/a", Collections.emptyList()));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
