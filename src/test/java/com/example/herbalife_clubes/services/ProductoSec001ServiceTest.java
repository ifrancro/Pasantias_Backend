package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.producto.ProductoConDisponibilidadDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.ProductoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ProductoSec001ServiceTest {

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
    void adminPuedeActivarGlobal() {
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(admin()));
        Producto producto = productoGlobal(false);
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO dto = productoService.activarProducto(10, 7);

        assertTrue(dto.getActivo());
        assertTrue(producto.getActivo());
    }

    @Test
    void adminPuedeDesactivarGlobal() {
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(admin()));
        Producto producto = productoGlobal(true);
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO dto = productoService.desactivarProducto(10, 7);

        assertFalse(dto.getActivo());
        assertFalse(producto.getActivo());
    }

    @Test
    void anfitrionNoPuedeDesactivar() {
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(anfitrion(20)));

        assertThrows(AccessDeniedException.class, () -> productoService.desactivarProducto(10, 20));
        verify(productoRepository, never()).save(any());
    }

    @Test
    void socioNoPuedeActivar() {
        when(usuarioRepository.findById(40)).thenReturn(Optional.of(usuario(40, "SOCIO")));

        assertThrows(AccessDeniedException.class, () -> productoService.activarProducto(10, 40));
        verify(productoRepository, never()).save(any());
    }

    @Test
    void usuarioBasicoNoPuedeDesactivar() {
        when(usuarioRepository.findById(41)).thenReturn(Optional.of(usuario(41, "USUARIO_BASICO")));

        assertThrows(AccessDeniedException.class, () -> productoService.desactivarProducto(10, 41));
        verify(productoRepository, never()).save(any());
    }

    @Test
    void adminProductoInexistenteLanza404() {
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(admin()));
        when(productoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productoService.activarProducto(999, 7));
    }

    @Test
    void anfitrionPutNoApagaActivo() {
        Usuario host = anfitrion(20);
        Producto producto = productoLocal(host, "APROBADO", true);
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO request = new ProductoDTO();
        request.setNombre("Batido v2");
        request.setDescripcion("Corregido");
        request.setIngredientes("proteína");
        request.setPuntosValor(12);
        request.setActivo(false);

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals("Batido v2", dto.getNombre());
        assertTrue(producto.getActivo());
        assertTrue(dto.getActivo());
        assertEquals("PENDIENTE", dto.getEstadoAprobacion());
        assertNull(producto.getRevisadoPor());
        assertNull(producto.getRevisadoAt());
    }

    @Test
    void anfitrionPutNoReactivaActivo() {
        Usuario host = anfitrion(20);
        Producto producto = productoLocal(host, "APROBADO", false);
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO request = new ProductoDTO();
        request.setNombre("Batido");
        request.setDescripcion("Desc");
        request.setActivo(true);

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertFalse(producto.getActivo());
        assertFalse(dto.getActivo());
        assertEquals("PENDIENTE", dto.getEstadoAprobacion());
    }

    @Test
    void adminPutSiPuedeCambiarActivo() {
        Producto producto = productoGlobal(true);
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(admin()));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO request = new ProductoDTO();
        request.setNombre("Shake");
        request.setDescripcion("Global");
        request.setActivo(false);

        ProductoDTO dto = productoService.updateProducto(10, request, 7);

        assertFalse(producto.getActivo());
        assertFalse(dto.getActivo());
    }

    @Test
    void toggleLocalPropioAprobadoCambiaDisponibleNoActivo() {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        Producto producto = productoLocal(host, "APROBADO", true);
        producto.setClubCreador(club);
        ClubProducto relacion = new ClubProducto();
        relacion.setClub(club);
        relacion.setProducto(producto);
        relacion.setDisponible(true);

        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.of(relacion));
        when(clubProductoRepository.save(any(ClubProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoConDisponibilidadDTO dto = productoService.toggleDisponibilidadEnClub(3, 10);

        assertFalse(dto.getDisponible());
        assertTrue(dto.getActivo());
        assertTrue(producto.getActivo());
        assertFalse(relacion.getDisponible());
    }

    @Test
    void toggleLocalDeOtroClubLanza403() {
        Usuario host = anfitrion(20);
        Club club3 = club(3, host, hub(1));
        Club club5 = club(5, anfitrion(99), hub(1));
        Producto ajeno = productoLocal(anfitrion(99), "APROBADO", true);
        ajeno.setClubCreador(club5);

        when(clubRepository.findById(3)).thenReturn(Optional.of(club3));
        when(productoRepository.findById(10)).thenReturn(Optional.of(ajeno));

        assertThrows(AccessDeniedException.class,
                () -> productoService.toggleDisponibilidadEnClub(3, 10));
        verify(clubProductoRepository, never()).save(any());
        assertTrue(ajeno.getActivo());
    }

    @Test
    void togglePendienteLanza400() {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        Producto pendiente = productoLocal(host, "PENDIENTE", true);
        pendiente.setClubCreador(club);

        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findById(10)).thenReturn(Optional.of(pendiente));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.toggleDisponibilidadEnClub(3, 10));
        assertTrue(ex.getMessage().toLowerCase().contains("aprobado"));
    }

    @Test
    void toggleRechazadoLanza400() {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        Producto rechazado = productoLocal(host, "RECHAZADO", true);
        rechazado.setClubCreador(club);

        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findById(10)).thenReturn(Optional.of(rechazado));

        assertThrows(IllegalArgumentException.class,
                () -> productoService.toggleDisponibilidadEnClub(3, 10));
    }

    @Test
    void toggleOtroHubLanza400() {
        Usuario host = anfitrion(20);
        Club club = club(3, host, hub(1));
        Producto deOtroHub = productoGlobal(true);
        deOtroHub.setHub(hub(2));

        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findById(10)).thenReturn(Optional.of(deOtroHub));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.toggleDisponibilidadEnClub(3, 10));
        assertTrue(ex.getMessage().toLowerCase().contains("hub"));
    }

    @Test
    void menuPublicoMuestraAprobadoActivoDisponible() {
        stubMenuClub(productoGlobal(true), true);

        List<ProductoDTO> dtos = productoService.getProductosByClubPublico(3);

        assertEquals(1, dtos.size());
        assertEquals(10, dtos.get(0).getId());
    }

    @Test
    void menuPublicoOcultaActivoFalseAunqueDisponible() {
        Producto inactivo = productoGlobal(false);
        stubMenuClub(inactivo, true);

        List<ProductoDTO> dtos = productoService.getProductosByClubPublico(3);

        assertTrue(dtos.isEmpty());
    }

    @Test
    void menuPublicoOcultaRechazado() {
        when(clubRepository.findById(3)).thenReturn(Optional.of(club(3, anfitrion(20), hub(1))));
        when(productoRepository.findByHubIdAndTipoAndEstadoAprobacion(1, "GLOBAL", "APROBADO"))
                .thenReturn(List.of());
        when(productoRepository.findByClubCreadorIdAndTipoAndEstadoAprobacion(3, "LOCAL", "APROBADO"))
                .thenReturn(List.of());

        assertTrue(productoService.getProductosByClubPublico(3).isEmpty());
    }

    @Test
    void getProductosPublicosSoloAprobadoActivo() {
        Producto ok = productoGlobal(true);
        Producto inactivo = productoGlobal(false);
        inactivo.setId(11);
        Producto rechazado = productoGlobal(true);
        rechazado.setId(12);
        rechazado.setEstadoAprobacion("RECHAZADO");
        Producto pendiente = productoGlobal(true);
        pendiente.setId(13);
        pendiente.setEstadoAprobacion("PENDIENTE");
        when(productoRepository.findAll()).thenReturn(List.of(ok, inactivo, rechazado, pendiente));

        List<ProductoDTO> dtos = productoService.getProductosPublicos();

        assertEquals(1, dtos.size());
        assertEquals(10, dtos.get(0).getId());
    }

    @Test
    void getProductoPublicoActivoFalseNoEsAccesible() {
        when(productoRepository.findById(10)).thenReturn(Optional.of(productoGlobal(false)));

        assertThrows(ResourceNotFoundException.class, () -> productoService.getProductoPublico(10));
    }

    @Test
    void getProductoPublicoRechazadoNoEsAccesible() {
        Producto rechazado = productoGlobal(true);
        rechazado.setEstadoAprobacion("RECHAZADO");
        when(productoRepository.findById(10)).thenReturn(Optional.of(rechazado));

        assertThrows(ResourceNotFoundException.class, () -> productoService.getProductoPublico(10));
    }

    @Test
    void getProductoPublicoPendienteNoEsAccesible() {
        Producto pendiente = productoGlobal(true);
        pendiente.setEstadoAprobacion("PENDIENTE");
        when(productoRepository.findById(10)).thenReturn(Optional.of(pendiente));

        assertThrows(ResourceNotFoundException.class, () -> productoService.getProductoPublico(10));
    }

    @Test
    void menuPublicoOptOutSinFilaSigueMostrandoGlobalActivo() {
        stubMenuClub(productoGlobal(true), null);

        List<ProductoDTO> dtos = productoService.getProductosByClubPublico(3);

        assertEquals(1, dtos.size());
    }

    private void stubMenuClub(Producto producto, Boolean disponible) {
        Club club = club(3, anfitrion(20), hub(1));
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findByHubIdAndTipoAndEstadoAprobacion(1, "GLOBAL", "APROBADO"))
                .thenReturn("GLOBAL".equals(producto.getTipo()) && "APROBADO".equals(producto.getEstadoAprobacion())
                        ? List.of(producto)
                        : List.of());
        when(productoRepository.findByClubCreadorIdAndTipoAndEstadoAprobacion(3, "LOCAL", "APROBADO"))
                .thenReturn(List.of());
        boolean visible = "APROBADO".equalsIgnoreCase(producto.getEstadoAprobacion())
                && Boolean.TRUE.equals(producto.getActivo());
        if (!visible) {
            return;
        }
        if (disponible == null) {
            when(clubProductoRepository.findByClubIdAndProductoId(3, producto.getId()))
                    .thenReturn(Optional.empty());
        } else {
            ClubProducto cp = new ClubProducto();
            cp.setDisponible(disponible);
            when(clubProductoRepository.findByClubIdAndProductoId(3, producto.getId()))
                    .thenReturn(Optional.of(cp));
        }
    }

    private static Usuario admin() {
        return usuario(7, "ADMIN");
    }

    private static Usuario anfitrion(int id) {
        return usuario(id, "ANFITRION");
    }

    private static Usuario usuario(int id, String rolNombre) {
        Rol rol = new Rol();
        rol.setNombre(rolNombre);
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        usuario.setNombre("User");
        usuario.setApellido(rolNombre);
        usuario.setEmail(id + "@test.com");
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

    private static Producto productoGlobal(boolean activo) {
        Producto producto = new Producto();
        producto.setId(10);
        producto.setNombre("Shake");
        producto.setHub(hub(1));
        producto.setTipo("GLOBAL");
        producto.setEstadoAprobacion("APROBADO");
        producto.setActivo(activo);
        producto.setPrecio(BigDecimal.ZERO);
        return producto;
    }

    private static Producto productoLocal(Usuario host, String estado, boolean activo) {
        Producto producto = new Producto();
        producto.setId(10);
        producto.setNombre("Batido");
        producto.setHub(hub(1));
        producto.setClubCreador(club(3, host, hub(1)));
        producto.setTipo("LOCAL");
        producto.setEstadoAprobacion(estado);
        producto.setActivo(activo);
        producto.setPrecio(BigDecimal.ZERO);
        return producto;
    }
}
