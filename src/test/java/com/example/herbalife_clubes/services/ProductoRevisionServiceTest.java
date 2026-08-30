package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoRevisionServiceTest {

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
    void anfitrionCreaProductoLocalPendiente() {
        Usuario host = anfitrion(20);
        Hub hub = hub();
        Club club = clubDelAnfitrion(host);
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
        request.setDescripcion("Proteico");
        request.setIngredientes("proteína");
        request.setPuntosValor(10);
        request.setPrecio(BigDecimal.ZERO);

        ProductoDTO dto = productoService.createProducto(request, 20, 1);

        assertEquals(10, dto.getId());
        assertEquals("LOCAL", dto.getTipo());
        assertEquals("PENDIENTE", dto.getEstadoAprobacion());
        assertEquals(5, dto.getClubCreadorId());
        assertNull(dto.getComentarioRevision());
        assertNull(dto.getRevisadoPorUsuarioId());
        assertNull(dto.getRevisadoAt());
    }

    @Test
    void adminApruebaConRevisorYFecha() {
        Usuario admin = admin(7);
        Producto producto = productoLocalRechazado();
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(admin));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO dto = productoService.cambiarEstadoAprobacion(10, "APROBADO", null, 7);

        assertEquals("APROBADO", dto.getEstadoAprobacion());
        assertNull(dto.getComentarioRevision());
        assertEquals(7, dto.getRevisadoPorUsuarioId());
        assertEquals("Ana Admin", dto.getRevisadoPorNombre());
        assertNotNull(dto.getRevisadoAt());
    }

    @Test
    void adminRechazaConComentarioPersistido() {
        Usuario admin = admin(7);
        Producto producto = productoLocalPendiente();
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(admin));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO dto = productoService.cambiarEstadoAprobacion(10, "RECHAZADO", "  Faltan ingredientes  ", 7);

        assertEquals("RECHAZADO", dto.getEstadoAprobacion());
        assertEquals("Faltan ingredientes", dto.getComentarioRevision());
        assertEquals(7, dto.getRevisadoPorUsuarioId());
        assertNotNull(dto.getRevisadoAt());
    }

    @Test
    void aprobarTrasReenvioSinComentarioLimpiaComentarioDeRechazo() {
        Usuario host = anfitrion(20);
        Usuario admin = admin(7);
        Producto producto = productoLocalRechazado();
        producto.setComentarioRevision("Falta información");
        LocalDateTime rechazoAt = producto.getRevisadoAt();

        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(admin));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO reenviado = productoService.reenviarProducto(10, 20);
        assertEquals("PENDIENTE", reenviado.getEstadoAprobacion());
        assertEquals("Falta información", reenviado.getComentarioRevision());
        assertNull(reenviado.getRevisadoPorUsuarioId());
        assertNull(reenviado.getRevisadoAt());

        ProductoDTO aprobado = productoService.cambiarEstadoAprobacion(10, "APROBADO", null, 7);

        assertEquals("APROBADO", aprobado.getEstadoAprobacion());
        assertNull(aprobado.getComentarioRevision());
        assertEquals(7, aprobado.getRevisadoPorUsuarioId());
        assertEquals("Ana Admin", aprobado.getRevisadoPorNombre());
        assertNotNull(aprobado.getRevisadoAt());
        assertNotEquals(rechazoAt, aprobado.getRevisadoAt());
    }

    @Test
    void aprobarConComentarioNuevoLoPersiste() {
        Usuario admin = admin(7);
        Producto producto = productoLocalRechazado();
        producto.setComentarioRevision("Falta información");
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(admin));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO dto = productoService.cambiarEstadoAprobacion(10, "APROBADO", "  Listo para menú  ", 7);

        assertEquals("APROBADO", dto.getEstadoAprobacion());
        assertEquals("Listo para menú", dto.getComentarioRevision());
        assertEquals(7, dto.getRevisadoPorUsuarioId());
        assertNotNull(dto.getRevisadoAt());
    }

    @Test
    void adminRechazaSinComentarioLanzaValidacion() {
        Usuario admin = admin(7);
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(admin));
        when(productoRepository.findById(10)).thenReturn(Optional.of(productoLocalPendiente()));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> productoService.cambiarEstadoAprobacion(10, "RECHAZADO", "   ", 7));
        assertTrue(ex.getMessage().toLowerCase().contains("comentario"));
    }

    @Test
    void anfitrionPropietarioVeComentarioEnGetInterno() {
        Producto producto = productoLocalRechazado();
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));

        ProductoDTO dto = productoService.getProducto(10);

        assertEquals("Faltan ingredientes", dto.getComentarioRevision());
        assertEquals(7, dto.getRevisadoPorUsuarioId());
    }

    @Test
    void socioNoRecibeComentarioRevision() {
        Producto producto = productoLocalRechazado();
        producto.setEstadoAprobacion("APROBADO");
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));

        ProductoDTO dto = productoService.getProductoPublico(10);

        assertNull(dto.getComentarioRevision());
        assertNull(dto.getRevisadoPorUsuarioId());
        assertNull(dto.getRevisadoPorNombre());
        assertNull(dto.getRevisadoAt());
        assertNull(dto.getIngredientes());
    }

    @Test
    void anfitrionEditaRechazadoSinPasarAPendiente() {
        Usuario host = anfitrion(20);
        Producto producto = productoLocalRechazado();
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO request = new ProductoDTO();
        request.setNombre("Batido v2");
        request.setDescripcion("Corregido");
        request.setIngredientes("proteína, leche");
        request.setPuntosValor(12);
        request.setActivo(true);

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals("Batido v2", dto.getNombre());
        assertEquals("RECHAZADO", dto.getEstadoAprobacion());
        assertEquals("Faltan ingredientes", dto.getComentarioRevision());
    }

    @Test
    void propietarioReenviaRechazadoAPendienteYConservaComentario() {
        Usuario host = anfitrion(20);
        Producto producto = productoLocalRechazado();
        LocalDateTime previo = producto.getRevisadoAt();
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO dto = productoService.reenviarProducto(10, 20);

        assertEquals("PENDIENTE", dto.getEstadoAprobacion());
        assertEquals("Faltan ingredientes", dto.getComentarioRevision());
        assertNull(dto.getRevisadoPorUsuarioId());
        assertNull(dto.getRevisadoAt());
        assertNotEquals(previo, dto.getRevisadoAt());
    }

    @Test
    void otroAnfitrionNoPuedeReenviarProductoAjeno() {
        Usuario otro = anfitrion(99);
        when(usuarioRepository.findById(99)).thenReturn(Optional.of(otro));
        when(productoRepository.findById(10)).thenReturn(Optional.of(productoLocalRechazado()));

        assertThrows(AccessDeniedException.class, () -> productoService.reenviarProducto(10, 99));
    }

    @Test
    void otroAnfitrionNoPuedeEditarProductoAjeno() {
        Usuario otro = anfitrion(99);
        when(usuarioRepository.findById(99)).thenReturn(Optional.of(otro));
        when(productoRepository.findById(10)).thenReturn(Optional.of(productoLocalRechazado()));

        ProductoDTO request = new ProductoDTO();
        request.setNombre("Hack");
        assertThrows(AccessDeniedException.class, () -> productoService.updateProducto(10, request, 99));
    }

    @Test
    void noSePuedeReenviarAprobadoNiPendiente() {
        Usuario host = anfitrion(20);
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));

        Producto aprobado = productoLocalRechazado();
        aprobado.setEstadoAprobacion("APROBADO");
        when(productoRepository.findById(10)).thenReturn(Optional.of(aprobado));
        assertThrows(IllegalArgumentException.class, () -> productoService.reenviarProducto(10, 20));

        Producto pendiente = productoLocalPendiente();
        when(productoRepository.findById(11)).thenReturn(Optional.of(pendiente));
        assertThrows(IllegalArgumentException.class, () -> productoService.reenviarProducto(11, 20));
    }

    @Test
    void getProductosPendientesSigueFuncionando() {
        when(productoRepository.findByEstadoAprobacion("PENDIENTE"))
                .thenReturn(List.of(productoLocalPendiente()));

        List<ProductoDTO> dtos = productoService.getProductosPendientes();

        assertEquals(1, dtos.size());
        assertEquals("PENDIENTE", dtos.get(0).getEstadoAprobacion());
        assertEquals("Batido", dtos.get(0).getNombre());
    }

    private static Usuario admin(int id) {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        usuario.setNombre("Ana");
        usuario.setApellido("Admin");
        usuario.setEmail("ana@hub.com");
        return usuario;
    }

    private static Usuario anfitrion(int id) {
        Rol rol = new Rol();
        rol.setNombre("ANFITRION");
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        usuario.setNombre("Andrea");
        usuario.setApellido("Anfitriona");
        usuario.setEmail("host-" + id + "@club.com");
        return usuario;
    }

    private static Hub hub() {
        Hub hub = new Hub();
        hub.setId(1);
        hub.setNombre("HUB Santa Cruz");
        return hub;
    }

    private static Club clubDelAnfitrion(Usuario host) {
        Club club = new Club();
        club.setId(5);
        club.setNombreClub("Club Demo");
        club.setAnfitrion(host);
        club.setHub(hub());
        return club;
    }

    private static Producto productoLocalPendiente() {
        Producto producto = new Producto();
        producto.setId(10);
        producto.setHub(hub());
        producto.setClubCreador(clubDelAnfitrion(anfitrion(20)));
        producto.setNombre("Batido");
        producto.setTipo("LOCAL");
        producto.setEstadoAprobacion("PENDIENTE");
        producto.setActivo(true);
        return producto;
    }

    private static Producto productoLocalRechazado() {
        Producto producto = productoLocalPendiente();
        producto.setEstadoAprobacion("RECHAZADO");
        producto.setComentarioRevision("Faltan ingredientes");
        producto.setRevisadoPor(admin(7));
        producto.setRevisadoAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        return producto;
    }
}
