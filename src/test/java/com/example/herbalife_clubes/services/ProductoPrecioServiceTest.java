package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.pricing.PrecioEfectivo;
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.ProductoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoPrecioServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ClubRepository clubRepository;
    @Mock
    private HubRepository hubRepository;
    @Mock
    private ClubProductoRepository clubProductoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ProductoServiceImpl productoService;

    @Test
    void putSinPrecioPreservaPrecioExistente() {
        Usuario host = anfitrion(20);
        Producto producto = productoLocal(host, "APROBADO", bd("25.00"));
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO request = basePut(producto);
        request.setPrecio(null);

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals(0, bd("25.00").compareTo(producto.getPrecio()));
        assertEquals(0, bd("25.00").compareTo(dto.getPrecio()));
        assertEquals("APROBADO", producto.getEstadoAprobacion());
    }

    @Test
    void putConPrecioActualizaSinPasarAPendiente() {
        Usuario host = anfitrion(20);
        Producto producto = productoLocal(host, "APROBADO", bd("25.00"));
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO request = basePut(producto);
        request.setPrecio(bd("30.00"));

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals(0, bd("30.00").compareTo(producto.getPrecio()));
        assertEquals(0, bd("30.00").compareTo(dto.getPrecio()));
        assertEquals("APROBADO", dto.getEstadoAprobacion());
    }

    @Test
    void putPrecioNegativoLanza400() {
        Usuario host = anfitrion(20);
        Producto producto = productoLocal(host, "APROBADO", bd("25.00"));
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));

        ProductoDTO request = basePut(producto);
        request.setPrecio(bd("-1.00"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.updateProducto(10, request, 20));
        assertTrue(ex.getMessage().toLowerCase().contains("negativo"));
        verify(productoRepository, never()).save(any());
    }

    @Test
    void hostCambiaPrecioVentaSinTocarDisponibleNiAprobacion() {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        Producto producto = productoGlobal(bd("25.00"));
        ClubProducto relacion = new ClubProducto();
        relacion.setClub(club);
        relacion.setProducto(producto);
        relacion.setDisponible(true);
        relacion.setPrecioVenta(null);

        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.of(relacion));
        when(clubProductoRepository.save(any(ClubProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO dto = productoService.actualizarPrecioVentaEnClub(3, 10, bd("28.50"));

        assertEquals(0, bd("28.50").compareTo(relacion.getPrecioVenta()));
        assertTrue(relacion.getDisponible());
        assertEquals("APROBADO", producto.getEstadoAprobacion());
        assertEquals(0, bd("25.00").compareTo(producto.getPrecio()));
        assertEquals(0, bd("25.00").compareTo(dto.getPrecio()));
        assertEquals(0, bd("28.50").compareTo(dto.getPrecioEfectivo()));
        assertEquals(0, bd("28.50").compareTo(dto.getPrecioVentaClub()));
        assertNull(producto.getRevisadoPor());
        assertNull(producto.getRevisadoAt());
    }

    @Test
    void nullEliminaOverrideYVuelveAlPrecioBase() {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        Producto producto = productoGlobal(bd("25.00"));
        ClubProducto relacion = new ClubProducto();
        relacion.setClub(club);
        relacion.setProducto(producto);
        relacion.setDisponible(false);
        relacion.setPrecioVenta(bd("28.00"));

        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.of(relacion));
        when(clubProductoRepository.save(any(ClubProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO dto = productoService.actualizarPrecioVentaEnClub(3, 10, null);

        assertNull(relacion.getPrecioVenta());
        assertFalse(relacion.getDisponible());
        assertEquals(0, bd("25.00").compareTo(dto.getPrecioEfectivo()));
        assertNull(dto.getPrecioVentaClub());
    }

    @Test
    void overrideEnGlobalSinFilaCreaClubProductoDisponibleTrue() {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        Producto producto = productoGlobal(bd("25.00"));

        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.empty());
        when(clubProductoRepository.save(any(ClubProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        productoService.actualizarPrecioVentaEnClub(3, 10, bd("28.00"));

        ArgumentCaptor<ClubProducto> captor = ArgumentCaptor.forClass(ClubProducto.class);
        verify(clubProductoRepository).save(captor.capture());
        assertTrue(captor.getValue().getDisponible());
        assertEquals(0, bd("28.00").compareTo(captor.getValue().getPrecioVenta()));
    }

    @Test
    void localDeOtroClubLanza403() {
        Usuario host = anfitrion(20);
        Club club3 = club(3, host, hub(1));
        Club club5 = club(5, anfitrion(99), hub(1));
        Producto ajeno = productoLocal(anfitrion(99), "APROBADO", bd("25.00"));
        ajeno.setClubCreador(club5);

        when(clubRepository.findById(3)).thenReturn(Optional.of(club3));
        when(productoRepository.findById(10)).thenReturn(Optional.of(ajeno));

        assertThrows(AccessDeniedException.class,
                () -> productoService.actualizarPrecioVentaEnClub(3, 10, bd("28.00")));
        verify(clubProductoRepository, never()).save(any());
    }

    @Test
    void productoDeOtroHubSeRechaza() {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        Producto deOtroHub = productoGlobal(bd("25.00"));
        deOtroHub.setHub(hub(2));

        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findById(10)).thenReturn(Optional.of(deOtroHub));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.actualizarPrecioVentaEnClub(3, 10, bd("28.00")));
        assertTrue(ex.getMessage().toLowerCase().contains("hub"));
    }

    @Test
    void socioMenuGlobalConOverrideDevuelveBaseYEfectivo() {
        stubMenuGlobal(productoGlobal(bd("25.00")), bd("28.00"), true);

        ProductoDTO dto = productoService.getProductosByClubPublico(3).get(0);

        assertEquals(0, bd("25.00").compareTo(dto.getPrecio()));
        assertEquals(0, bd("28.00").compareTo(dto.getPrecioEfectivo()));
        assertNull(dto.getPrecioVentaClub());
    }

    @Test
    void socioMenuGlobalSinOverrideUsaPrecioBase() {
        stubMenuGlobal(productoGlobal(bd("25.00")), null, true);

        ProductoDTO dto = productoService.getProductosByClubPublico(3).get(0);

        assertEquals(0, bd("25.00").compareTo(dto.getPrecio()));
        assertEquals(0, bd("25.00").compareTo(dto.getPrecioEfectivo()));
    }

    @Test
    void socioMenuGlobalSinFilaUsaPrecioBase() {
        stubMenuGlobalSinFila(productoGlobal(bd("25.00")));

        ProductoDTO dto = productoService.getProductosByClubPublico(3).get(0);

        assertEquals(0, bd("25.00").compareTo(dto.getPrecioEfectivo()));
    }

    @Test
    void socioMenuLocalIgnoraOverrideAccidental() {
        Usuario host = anfitrion(20);
        Producto local = productoLocal(host, "APROBADO", bd("20.00"));
        stubMenuLocal(local, bd("32.00"));

        ProductoDTO dto = productoService.getProductosByClubPublico(3).get(0);

        assertEquals("LOCAL", dto.getTipo());
        assertEquals(0, bd("20.00").compareTo(dto.getPrecio()));
        assertEquals(0, bd("20.00").compareTo(dto.getPrecioEfectivo()));
        assertNull(dto.getPrecioVentaClub());
    }

    @Test
    void patchLocalEscribeProductoPrecioYLimpiaOverrideAccidental() {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        Producto local = productoLocal(host, "APROBADO", bd("20.00"));
        ClubProducto relacion = new ClubProducto();
        relacion.setClub(club);
        relacion.setProducto(local);
        relacion.setDisponible(true);
        relacion.setPrecioVenta(bd("32.00"));

        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findById(10)).thenReturn(Optional.of(local));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.of(relacion));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clubProductoRepository.save(any(ClubProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO dto = productoService.actualizarPrecioVentaEnClub(3, 10, bd("32.00"));

        assertEquals(0, bd("32.00").compareTo(local.getPrecio()));
        assertNull(relacion.getPrecioVenta());
        assertEquals("APROBADO", local.getEstadoAprobacion());
        assertEquals(0, bd("32.00").compareTo(dto.getPrecio()));
        assertEquals(0, bd("32.00").compareTo(dto.getPrecioEfectivo()));
        assertNull(dto.getPrecioVentaClub());
        verify(productoRepository).save(local);
        verify(clubProductoRepository).save(relacion);
    }

    @Test
    void patchLocalNullRechaza400() {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        Producto local = productoLocal(host, "APROBADO", bd("20.00"));

        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findById(10)).thenReturn(Optional.of(local));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.actualizarPrecioVentaEnClub(3, 10, null));
        assertEquals(PrecioEfectivo.MENSAJE_PRECIO_LOCAL_OBLIGATORIO, ex.getMessage());
        verify(productoRepository, never()).save(any());
        verify(clubProductoRepository, never()).save(any());
    }

    @Test
    void patchLocalNoCambiaAprobacionNiEscribeClubProductoPrecioVenta() {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        Producto local = productoLocal(host, "APROBADO", bd("20.00"));

        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findById(10)).thenReturn(Optional.of(local));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.empty());
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        productoService.actualizarPrecioVentaEnClub(3, 10, bd("25.00"));

        assertEquals("APROBADO", local.getEstadoAprobacion());
        verify(clubProductoRepository, never()).save(any());
    }

    @Test
    void hostListaLocalNoExponePrecioVentaClub() {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        Producto local = productoLocal(host, "APROBADO", bd("32.00"));
        ClubProducto cp = new ClubProducto();
        cp.setDisponible(true);
        cp.setPrecioVenta(bd("99.00"));

        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findByHubIdAndTipoAndEstadoAprobacion(1, "GLOBAL", "APROBADO"))
                .thenReturn(List.of());
        when(productoRepository.findByClubCreadorId(3)).thenReturn(List.of(local));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.of(cp));

        ProductoDTO dto = productoService.getProductosByClubParaAnfitrion(3).get(0);

        assertEquals(0, bd("32.00").compareTo(dto.getPrecio()));
        assertEquals(0, bd("32.00").compareTo(dto.getPrecioEfectivo()));
        assertNull(dto.getPrecioVentaClub());
    }

    @Test
    void socioMenuProductoViejoConPrecioCero() {
        stubMenuGlobal(productoGlobal(BigDecimal.ZERO), null, true);

        ProductoDTO dto = productoService.getProductosByClubPublico(3).get(0);

        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getPrecio()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getPrecioEfectivo()));
    }

    @Test
    void hostListaVePrecioVentaClub() {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        Producto producto = productoGlobal(bd("25.00"));
        ClubProducto cp = new ClubProducto();
        cp.setDisponible(true);
        cp.setPrecioVenta(bd("28.00"));

        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findByHubIdAndTipoAndEstadoAprobacion(1, "GLOBAL", "APROBADO"))
                .thenReturn(List.of(producto));
        when(productoRepository.findByClubCreadorId(3)).thenReturn(List.of());
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.of(cp));

        ProductoDTO dto = productoService.getProductosByClubParaAnfitrion(3).get(0);

        assertEquals(0, bd("25.00").compareTo(dto.getPrecio()));
        assertEquals(0, bd("28.00").compareTo(dto.getPrecioVentaClub()));
        assertEquals(0, bd("28.00").compareTo(dto.getPrecioEfectivo()));
        assertTrue(dto.getDisponible());
    }

    @Test
    void localNuevoNoDuplicaPrecioEnClubProductos() {
        Usuario host = anfitrion(20);
        Hub hub = hub(1);
        Club club = club(5, host, hub);
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(hubRepository.findById(1)).thenReturn(Optional.of(hub));
        when(clubRepository.findByAnfitrionId(20)).thenReturn(List.of(club));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            p.setId(10);
            return p;
        });
        when(clubProductoRepository.findByClubIdAndProductoId(5, 10)).thenReturn(Optional.empty());
        when(clubProductoRepository.save(any(ClubProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO request = new ProductoDTO();
        request.setNombre("Batido");
        request.setPrecio(bd("25.00"));

        productoService.createProducto(request, 20, 1);

        ArgumentCaptor<ClubProducto> captor = ArgumentCaptor.forClass(ClubProducto.class);
        verify(clubProductoRepository).save(captor.capture());
        assertNull(captor.getValue().getPrecioVenta());
        assertTrue(captor.getValue().getDisponible());
    }

    private void stubMenuGlobal(Producto producto, BigDecimal precioVenta, boolean disponible) {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findByHubIdAndTipoAndEstadoAprobacion(1, "GLOBAL", "APROBADO"))
                .thenReturn(List.of(producto));
        when(productoRepository.findByClubCreadorIdAndTipoAndEstadoAprobacion(3, "LOCAL", "APROBADO"))
                .thenReturn(List.of());
        ClubProducto cp = new ClubProducto();
        cp.setDisponible(disponible);
        cp.setPrecioVenta(precioVenta);
        when(clubProductoRepository.findByClubIdAndProductoId(3, producto.getId())).thenReturn(Optional.of(cp));
    }

    private void stubMenuGlobalSinFila(Producto producto) {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findByHubIdAndTipoAndEstadoAprobacion(1, "GLOBAL", "APROBADO"))
                .thenReturn(List.of(producto));
        when(productoRepository.findByClubCreadorIdAndTipoAndEstadoAprobacion(3, "LOCAL", "APROBADO"))
                .thenReturn(List.of());
        when(clubProductoRepository.findByClubIdAndProductoId(3, producto.getId())).thenReturn(Optional.empty());
    }

    private void stubMenuLocal(Producto producto, BigDecimal precioVenta) {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findByHubIdAndTipoAndEstadoAprobacion(1, "GLOBAL", "APROBADO"))
                .thenReturn(List.of());
        when(productoRepository.findByClubCreadorIdAndTipoAndEstadoAprobacion(3, "LOCAL", "APROBADO"))
                .thenReturn(List.of(producto));
        ClubProducto cp = new ClubProducto();
        cp.setDisponible(true);
        cp.setPrecioVenta(precioVenta);
        when(clubProductoRepository.findByClubIdAndProductoId(3, producto.getId())).thenReturn(Optional.of(cp));
    }

    private static ProductoDTO basePut(Producto producto) {
        ProductoDTO request = new ProductoDTO();
        request.setNombre(producto.getNombre());
        request.setDescripcion(producto.getDescripcion());
        request.setIngredientes(producto.getIngredientes());
        request.setPuntosValor(producto.getPuntosValor());
        return request;
    }

    private static Usuario anfitrion(int id) {
        Rol rol = new Rol();
        rol.setNombre("ANFITRION");
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        usuario.setNombre("Host");
        usuario.setApellido("" + id);
        return usuario;
    }

    private static Hub hub(int id) {
        Hub hub = new Hub();
        hub.setId(id);
        hub.setNombre("HUB " + id);
        return hub;
    }

    private static Club club(int id, Usuario host, Hub hub) {
        Club club = new Club();
        club.setId(id);
        club.setNombreClub("Club " + id);
        club.setAnfitrion(host);
        club.setHub(hub);
        return club;
    }

    private static Producto productoGlobal(BigDecimal precio) {
        Producto producto = new Producto();
        producto.setId(10);
        producto.setNombre("Shake");
        producto.setHub(hub(1));
        producto.setTipo("GLOBAL");
        producto.setEstadoAprobacion("APROBADO");
        producto.setActivo(true);
        producto.setPrecio(precio);
        return producto;
    }

    private static Producto productoLocal(Usuario host, String estado, BigDecimal precio) {
        Producto producto = new Producto();
        producto.setId(10);
        producto.setNombre("Batido");
        producto.setDescripcion("Proteico");
        producto.setIngredientes("proteína");
        producto.setPuntosValor(10);
        producto.setHub(hub(1));
        producto.setClubCreador(club(3, host, hub(1)));
        producto.setTipo("LOCAL");
        producto.setEstadoAprobacion(estado);
        producto.setActivo(true);
        producto.setPrecio(precio);
        return producto;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
