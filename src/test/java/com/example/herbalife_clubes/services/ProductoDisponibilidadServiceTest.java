package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoConDisponibilidadDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.OrderCreationRejectedException;
import com.example.herbalife_clubes.exceptions.ProductAvailabilityRejectedException;
import com.example.herbalife_clubes.pedidos.OrderCreationRejections;
import com.example.herbalife_clubes.pricing.PrecioEfectivo;
import com.example.herbalife_clubes.productos.ProductAvailabilityRejections;
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.ComboRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.NotificacionRepository;
import com.example.herbalife_clubes.repositories.PedidoRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.PedidoServiceImpl;
import com.example.herbalife_clubes.serviceimpls.ProductoServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PROD-AVAIL-002: menú socio opt-in + precio válido + toggle con precio.
 */
@ExtendWith(MockitoExtension.class)
class ProductoDisponibilidadServiceTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private ClubRepository clubRepository;
    @Mock private HubRepository hubRepository;
    @Mock private ClubProductoRepository clubProductoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private MembresiaRepository membresiaRepository;
    @Mock private NotificacionRepository notificacionRepository;
    @Mock private ComboRepository comboRepository;

    @InjectMocks
    private ProductoServiceImpl productoService;

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // --- MENÚ SOCIO GLOBAL ---

    @Test
    void globalSinClubProductoNoAparece() {
        stubMenuGlobal(productoGlobal(bd("22.00")), Optional.empty());
        assertTrue(productoService.getProductosByClubPublico(3).isEmpty());
    }

    @Test
    void globalClubProductoFalseNoAparece() {
        stubMenuGlobal(productoGlobal(bd("22.00")), Optional.of(cp(false, null)));
        assertTrue(productoService.getProductosByClubPublico(3).isEmpty());
    }

    @Test
    void globalClubProductoTrueConPrecioAparece() {
        stubMenuGlobal(productoGlobal(bd("22.00")), Optional.of(cp(true, null)));
        List<ProductoDTO> menu = productoService.getProductosByClubPublico(3);
        assertEquals(1, menu.size());
        assertEquals(0, bd("22.00").compareTo(menu.get(0).getPrecioEfectivo()));
    }

    @Test
    void globalClubProductoTruePrecioNullNoAparece() {
        Producto sinPrecio = productoGlobal(null);
        stubMenuGlobal(sinPrecio, Optional.of(cp(true, null)));
        assertTrue(productoService.getProductosByClubPublico(3).isEmpty());
    }

    @Test
    void globalClubProductoTruePrecioCeroNoAparece() {
        stubMenuGlobal(productoGlobal(BigDecimal.ZERO), Optional.of(cp(true, null)));
        assertTrue(productoService.getProductosByClubPublico(3).isEmpty());
    }

    @Test
    void productoInactivoNoAparece() {
        Producto inactivo = productoGlobal(bd("22.00"));
        inactivo.setActivo(false);
        when(clubRepository.findById(3)).thenReturn(Optional.of(club()));
        when(productoRepository.findByHubIdAndTipoAndEstadoAprobacion(1, "GLOBAL", "APROBADO"))
                .thenReturn(List.of(inactivo));
        when(productoRepository.findByClubCreadorIdAndTipoAndEstadoAprobacion(3, "LOCAL", "APROBADO"))
                .thenReturn(List.of());
        assertTrue(productoService.getProductosByClubPublico(3).isEmpty());
    }

    @Test
    void productoNoAprobadoNoApareceEnMenu() {
        when(clubRepository.findById(3)).thenReturn(Optional.of(club()));
        when(productoRepository.findByHubIdAndTipoAndEstadoAprobacion(1, "GLOBAL", "APROBADO"))
                .thenReturn(List.of());
        when(productoRepository.findByClubCreadorIdAndTipoAndEstadoAprobacion(3, "LOCAL", "APROBADO"))
                .thenReturn(List.of());
        assertTrue(productoService.getProductosByClubPublico(3).isEmpty());
    }

    // --- LOCAL ---

    @Test
    void localAprobadoDisponibleConPrecioAparece() {
        stubMenuLocal(productoLocal(bd("18.00")), Optional.of(cp(true, null)));
        assertEquals(1, productoService.getProductosByClubPublico(3).size());
    }

    @Test
    void localDisponibleFalseNoAparece() {
        stubMenuLocal(productoLocal(bd("18.00")), Optional.of(cp(false, null)));
        assertTrue(productoService.getProductosByClubPublico(3).isEmpty());
    }

    @Test
    void localSinPrecioNoAparece() {
        stubMenuLocal(productoLocal(null), Optional.of(cp(true, null)));
        assertTrue(productoService.getProductosByClubPublico(3).isEmpty());
    }

    // --- HOST ---

    @Test
    void hostCatalogoIncluyeProductoSinRelacion() {
        Producto global = productoGlobal(bd("22.00"));
        when(clubRepository.findById(3)).thenReturn(Optional.of(club()));
        when(productoRepository.findByHubIdAndTipoAndEstadoAprobacion(1, "GLOBAL", "APROBADO"))
                .thenReturn(List.of(global));
        when(productoRepository.findByClubCreadorId(3)).thenReturn(List.of());
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.empty());

        List<ProductoDTO> host = productoService.getProductosByClubParaAnfitrion(3);

        assertEquals(1, host.size());
        assertNull(host.get(0).getDisponible());
    }

    @Test
    void hostVeProductoOffSinRelacion() {
        Producto global = productoGlobal(bd("22.00"));
        when(productoRepository.findByHubId(1)).thenReturn(List.of(global));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.empty());

        List<ProductoConDisponibilidadDTO> host = productoService.getProductosByHub(1, 3);

        assertEquals(1, host.size());
        assertNull(host.get(0).getDisponible());
    }

    // --- TOGGLE ---

    @Test
    void toggleOffAOnConPrecioOk() {
        Producto producto = productoGlobal(bd("22.00"));
        Club club = club();
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.empty());
        when(clubProductoRepository.save(any(ClubProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoConDisponibilidadDTO dto = productoService.toggleDisponibilidadEnClub(3, 10);

        assertTrue(dto.getDisponible());
        verify(clubProductoRepository).save(any(ClubProducto.class));
    }

    @Test
    void toggleSinRelacionCreaRelacionDisponibleTrue() {
        Producto producto = productoGlobal(bd("22.00"));
        when(clubRepository.findById(3)).thenReturn(Optional.of(club()));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.empty());
        when(clubProductoRepository.save(any(ClubProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        productoService.toggleDisponibilidadEnClub(3, 10);

        verify(clubProductoRepository).save(any(ClubProducto.class));
    }

    @Test
    void toggleOffAOnSinPrecioRechaza409() {
        Producto producto = productoGlobal(null);
        when(clubRepository.findById(3)).thenReturn(Optional.of(club()));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        ClubProducto cp = cp(false, null);
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.of(cp));

        ProductAvailabilityRejectedException ex = assertThrows(ProductAvailabilityRejectedException.class,
                () -> productoService.toggleDisponibilidadEnClub(3, 10));
        assertEquals(ProductAvailabilityRejections.PRODUCT_PRICE_REQUIRED, ex.getErrorCode());
        assertFalse(cp.getDisponible());
        verify(clubProductoRepository, never()).save(any());
    }

    @Test
    void toggleOnAOffPermiteAunquePrecioFalte() {
        Producto producto = productoGlobal(null);
        Club club = club();
        ClubProducto cp = cp(true, null);
        cp.setClub(club);
        cp.setProducto(producto);
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.of(cp));
        when(clubProductoRepository.save(any(ClubProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoConDisponibilidadDTO dto = productoService.toggleDisponibilidadEnClub(3, 10);

        assertFalse(dto.getDisponible());
    }

    // --- PEDIDO ---

    @Test
    void pedidoSinRelacionExplicitaRechazado() {
        stubPedidoSocio(Optional.empty());
        OrderCreationRejectedException ex = assertThrows(OrderCreationRejectedException.class,
                () -> pedidoService.createPedido(new PedidoDTO(), 1, 3, 10));
        assertEquals(OrderCreationRejections.ORDER_PRODUCT_UNAVAILABLE, ex.getErrorCode());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void pedidoDisponibleFalseRechazado() {
        stubPedidoSocio(Optional.of(cp(false, bd("22.00"))));
        OrderCreationRejectedException ex = assertThrows(OrderCreationRejectedException.class,
                () -> pedidoService.createPedido(new PedidoDTO(), 1, 3, 10));
        assertEquals(OrderCreationRejections.ORDER_PRODUCT_UNAVAILABLE, ex.getErrorCode());
    }

    @Test
    void pedidoDisponibleTrueSinPrecioRechazado() {
        stubPedidoSocio(Optional.of(cp(true, null)));
        when(productoRepository.findById(10)).thenReturn(Optional.of(productoGlobal(null)));
        OrderCreationRejectedException ex = assertThrows(OrderCreationRejectedException.class,
                () -> pedidoService.createPedido(new PedidoDTO(), 1, 3, 10));
        assertEquals(OrderCreationRejections.ORDER_PRODUCT_UNAVAILABLE, ex.getErrorCode());
        assertEquals(PrecioEfectivo.MENSAJE_PRECIO_NO_CONFIGURADO, ex.getMessage());
    }

    @Test
    void pedidoHabilitadoConPrecioValidoPermitido() {
        stubPedidoSocio(Optional.of(cp(true, bd("22.00"))));
        when(pedidoRepository.save(any())).thenAnswer(inv -> {
            var p = inv.getArgument(0, com.example.herbalife_clubes.entities.Pedido.class);
            p.setId(99);
            return p;
        });
        PedidoDTO dto = pedidoService.createPedido(new PedidoDTO(), 1, 3, 10);
        assertEquals(99, dto.getId());
    }

    private void stubMenuGlobal(Producto producto, Optional<ClubProducto> cpOpt) {
        when(clubRepository.findById(3)).thenReturn(Optional.of(club()));
        when(productoRepository.findByHubIdAndTipoAndEstadoAprobacion(1, "GLOBAL", "APROBADO"))
                .thenReturn(List.of(producto));
        when(productoRepository.findByClubCreadorIdAndTipoAndEstadoAprobacion(3, "LOCAL", "APROBADO"))
                .thenReturn(List.of());
        when(clubProductoRepository.findByClubIdAndProductoId(3, producto.getId())).thenReturn(cpOpt);
    }

    private void stubMenuLocal(Producto producto, Optional<ClubProducto> cpOpt) {
        when(clubRepository.findById(3)).thenReturn(Optional.of(club()));
        when(productoRepository.findByHubIdAndTipoAndEstadoAprobacion(1, "GLOBAL", "APROBADO"))
                .thenReturn(List.of());
        when(productoRepository.findByClubCreadorIdAndTipoAndEstadoAprobacion(3, "LOCAL", "APROBADO"))
                .thenReturn(List.of(producto));
        when(clubProductoRepository.findByClubIdAndProductoId(3, producto.getId())).thenReturn(cpOpt);
    }

    private void stubPedidoSocio(Optional<ClubProducto> cpOpt) {
        Club club = club();
        club.setEstado("ACTIVO");
        Membresia membresia = new Membresia();
        membresia.setId(1);
        membresia.setEstado("ACTIVA");
        membresia.setClub(club);
        Usuario socio = new Usuario();
        socio.setId(5);
        socio.setEmail("socio@test.com");
        membresia.setUsuario(socio);
        Producto producto = productoGlobal(bd("22.00"));
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresia));
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(usuarioRepository.findByEmail("socio@test.com")).thenReturn(Optional.of(socio));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("socio@test.com", "n/a", Collections.emptyList()));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(cpOpt);
    }

    private static Club club() {
        Hub hub = new Hub();
        hub.setId(1);
        Club club = new Club();
        club.setId(3);
        club.setHub(hub);
        club.setEstado("ACTIVO");
        return club;
    }

    private static Producto productoGlobal(BigDecimal precio) {
        Producto p = new Producto();
        p.setId(10);
        p.setNombre("Batido Nutricional");
        p.setTipo("GLOBAL");
        p.setEstadoAprobacion("APROBADO");
        p.setActivo(true);
        p.setPrecio(precio);
        p.setHub(club().getHub());
        return p;
    }

    private static Producto productoLocal(BigDecimal precio) {
        Producto p = new Producto();
        p.setId(11);
        p.setNombre("Proteína");
        p.setTipo("LOCAL");
        p.setEstadoAprobacion("APROBADO");
        p.setActivo(true);
        p.setPrecio(precio);
        p.setHub(club().getHub());
        p.setClubCreador(club());
        return p;
    }

    private static ClubProducto cp(boolean disponible, BigDecimal precioVenta) {
        ClubProducto cp = new ClubProducto();
        cp.setDisponible(disponible);
        cp.setPrecioVenta(precioVenta);
        return cp;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
