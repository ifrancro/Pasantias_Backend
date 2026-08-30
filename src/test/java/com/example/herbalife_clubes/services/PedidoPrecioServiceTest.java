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
import com.example.herbalife_clubes.entities.PedidoItem;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.pricing.PrecioEfectivo;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoPrecioServiceTest {

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
    void pedidoSocioCongelaPrecioEfectivoYSubtotal() {
        Producto producto = stubSocio(bd("25.00"), bd("28.00"));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido pedido = inv.getArgument(0);
            pedido.setId(50);
            return pedido;
        });

        PedidoDTO dto = new PedidoDTO();
        dto.setCantidad(2);

        PedidoDTO creado = pedidoService.createPedido(dto, 1, 3, 10);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        PedidoItem item = captor.getValue().getItems().get(0);
        assertEquals(0, bd("28.00").compareTo(item.getPrecioUnitario()));
        assertEquals(0, bd("56.00").compareTo(item.getSubtotal()));
        assertEquals(producto.getId(), item.getProducto().getId());
        assertEquals(0, bd("28.00").compareTo(creado.getItems().get(0).getPrecioUnitario()));
        assertEquals(0, bd("56.00").compareTo(creado.getItems().get(0).getSubtotal()));
    }

    @Test
    void pedidoLocalCongelaProductoPrecioIgnorandoOverride() {
        stubSocioClub();
        Producto local = producto(10, "Frappe", bd("20.00"));
        local.setTipo("LOCAL");
        local.setEstadoAprobacion("APROBADO");
        local.setActivo(true);
        when(productoRepository.findById(10)).thenReturn(Optional.of(local));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10))
                .thenReturn(Optional.of(cp(local, true, bd("32.00"))));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido pedido = inv.getArgument(0);
            pedido.setId(52);
            return pedido;
        });

        PedidoDTO dto = new PedidoDTO();
        dto.setCantidad(2);

        PedidoDTO creado = pedidoService.createPedido(dto, 1, 3, 10);

        assertEquals(0, bd("20.00").compareTo(creado.getItems().get(0).getPrecioUnitario()));
        assertEquals(0, bd("40.00").compareTo(creado.getItems().get(0).getSubtotal()));
    }

    @Test
    void pedidoSocioMultiplesItemsUsaEfectivoDeCadaUno() {
        stubSocioClub();
        Producto a = producto(10, "A", bd("10.00"));
        a.setTipo("GLOBAL");
        a.setEstadoAprobacion("APROBADO");
        a.setActivo(true);
        Producto b = producto(11, "B", bd("20.00"));
        b.setTipo("GLOBAL");
        b.setEstadoAprobacion("APROBADO");
        b.setActivo(true);
        when(productoRepository.findById(10)).thenReturn(Optional.of(a));
        when(productoRepository.findById(11)).thenReturn(Optional.of(b));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10))
                .thenReturn(Optional.of(cp(a, true, bd("12.00"))));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 11))
                .thenReturn(Optional.of(cp(b, true, null)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido pedido = inv.getArgument(0);
            pedido.setId(51);
            return pedido;
        });

        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setItems(List.of(item(10, 2, bd("999")), item(11, 1, bd("999"))));

        PedidoDTO creado = pedidoService.createPedidoConItems(request, 1, 3);

        assertEquals(0, bd("12.00").compareTo(creado.getItems().get(0).getPrecioUnitario()));
        assertEquals(0, bd("24.00").compareTo(creado.getItems().get(0).getSubtotal()));
        assertEquals(0, bd("20.00").compareTo(creado.getItems().get(1).getPrecioUnitario()));
        assertEquals(0, bd("20.00").compareTo(creado.getItems().get(1).getSubtotal()));
    }

    @Test
    void clienteNoPuedeFalsificarPrecioUnitario() {
        stubSocio(bd("25.00"), bd("28.00"));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoItemDTO mentira = item(10, 1, bd("1.00"));
        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setItems(List.of(mentira));

        PedidoDTO creado = pedidoService.createPedidoConItems(request, 1, 3);

        assertEquals(0, bd("28.00").compareTo(creado.getItems().get(0).getPrecioUnitario()));
        assertNotEquals(0, bd("1.00").compareTo(creado.getItems().get(0).getPrecioUnitario()));
    }

    @Test
    void precioEfectivoCeroRechazaPedidoSocio() {
        stubSocio(bd("25.00"), BigDecimal.ZERO);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.createPedido(new PedidoDTO(), 1, 3, 10));
        assertEquals(PrecioEfectivo.MENSAJE_PRECIO_NO_CONFIGURADO, ex.getMessage());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void mostradorCongelaPrecioEfectivo() {
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
            pedido.setId(70);
            return pedido;
        });

        PedidoMostradorRequestDTO request = new PedidoMostradorRequestDTO();
        request.setClubId(3);
        request.setTipoPago("EFECTIVO");
        PedidoItemDTO itemMentira = item(10, 3, bd("1.00"));
        request.setItems(List.of(itemMentira));

        PedidoDTO creado = pedidoService.createPedidoMostrador(request);

        assertEquals(0, bd("28.00").compareTo(creado.getItems().get(0).getPrecioUnitario()));
        assertEquals(0, bd("84.00").compareTo(creado.getItems().get(0).getSubtotal()));
    }

    @Test
    void mostradorPrecioCeroSeRechaza() {
        authenticateAs("host@club.com");
        Usuario host = anfitrion(20);
        Club club = clubActivo(host);
        Producto producto = producto(10, "Shake", BigDecimal.ZERO);
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
                .thenReturn(Optional.of(cp(producto, true, null)));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));

        PedidoMostradorRequestDTO request = new PedidoMostradorRequestDTO();
        request.setClubId(3);
        request.setTipoPago("EFECTIVO");
        request.setItems(List.of(item(10, 1, null)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.createPedidoMostrador(request));
        assertEquals(PrecioEfectivo.MENSAJE_PRECIO_NO_CONFIGURADO, ex.getMessage());
        verify(pedidoRepository, never()).save(any());
    }

    private Producto stubSocio(BigDecimal precioBase, BigDecimal precioVenta) {
        stubSocioClub();
        Producto producto = producto(10, "Batido", precioBase);
        producto.setTipo("GLOBAL");
        producto.setEstadoAprobacion("APROBADO");
        producto.setActivo(true);
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10))
                .thenReturn(Optional.of(cp(producto, true, precioVenta)));
        return producto;
    }

    private void stubSocioClub() {
        Usuario host = anfitrion(20);
        Club club = clubActivo(host);
        Membresia membresia = new Membresia();
        membresia.setId(1);
        membresia.setEstado("ACTIVA");
        membresia.setNumeroSocio("SC-1");
        membresia.setClub(club);
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresia));
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
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
                new UsernamePasswordAuthenticationToken(email, "n/a"));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
