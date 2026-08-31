package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.common.PagedResponse;
import com.example.herbalife_clubes.dtos.pedido.PedidoConItemsDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.EstadoPedido;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.TipoConsumo;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.pedidos.PedidoComboSupport;
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.PedidoRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.PedidoServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SEC-ORDER-ACCESS-002: lectura/cancelación/estado de pedidos con ownership.
 */
@Tag("postgres")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.open-in-view=false"
})
@Testcontainers(disabledWithoutDocker = true)
@EnabledIf("dockerAvailable")
@Import({PedidoServiceImpl.class, PedidoComboSupport.class})
class PedidoAccessControlPersistenceTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Autowired private PedidoService pedidoService;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private MembresiaRepository membresiaRepository;
    @Autowired private ClubRepository clubRepository;
    @Autowired private HubRepository hubRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ClubProductoRepository clubProductoRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void socioAGetPedidoPropioOk() {
        AccessSeed seed = seedAccess();
        authenticate(seed.socioAEmail());

        PedidoDTO dto = pedidoService.getPedido(seed.pedidoAId());

        assertEquals(seed.pedidoAId(), dto.getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void socioAGetPedidoAjenoForbidden() {
        AccessSeed seed = seedAccess();
        authenticate(seed.socioAEmail());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> pedidoService.getPedido(seed.pedidoBId()));

        assertEquals("No tienes permisos para acceder a este pedido.", ex.getMessage());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void socioAListaMembresiaPropiaOk() {
        AccessSeed seed = seedAccess();
        authenticate(seed.socioAEmail());

        List<PedidoDTO> pedidos = pedidoService.getPedidosBySocio(seed.membresiaAId());

        assertEquals(1, pedidos.size());
        assertEquals(seed.pedidoAId(), pedidos.get(0).getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void socioAListaMembresiaAjenaForbidden() {
        AccessSeed seed = seedAccess();
        authenticate(seed.socioAEmail());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> pedidoService.getPedidosBySocio(seed.membresiaBId()));

        assertEquals("No tienes permisos para usar esta membresía.", ex.getMessage());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void socioAPaginadoMembresiaAjenaForbidden() {
        AccessSeed seed = seedAccess();
        authenticate(seed.socioAEmail());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> pedidoService.getPedidosBySocioPaginados(
                        seed.membresiaBId(), 0, 20, null, null, null));

        assertEquals("No tienes permisos para usar esta membresía.", ex.getMessage());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void socioACancelaPedidoPropioOk() {
        AccessSeed seed = seedAccess();
        authenticate(seed.socioAEmail());

        PedidoDTO dto = pedidoService.cancelarPedido(seed.pedidoAId());

        assertEquals("CANCELADO", dto.getEstado());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void socioACancelaPedidoAjenoForbiddenYEstadoIntacto() {
        AccessSeed seed = seedAccess();
        authenticate(seed.socioAEmail());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> pedidoService.cancelarPedido(seed.pedidoBId()));

        assertEquals("No tienes permisos para acceder a este pedido.", ex.getMessage());
        assertEquals(EstadoPedido.RECIBIDO, estadoPedido(seed.pedidoBId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void anfitrionAListaClubPropioOk() {
        AccessSeed seed = seedAccess();
        authenticate(seed.hostAEmail());

        List<PedidoDTO> pedidos = pedidoService.getPedidosByClub(seed.clubAId());

        assertEquals(1, pedidos.size());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void anfitrionAListaClubAjenoForbidden() {
        AccessSeed seed = seedAccess();
        authenticate(seed.hostAEmail());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> pedidoService.getPedidosByClub(seed.clubBId()));

        assertEquals("No tienes permisos para acceder a este club.", ex.getMessage());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void anfitrionAPaginadoClubAjenoForbidden() {
        AccessSeed seed = seedAccess();
        authenticate(seed.hostAEmail());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> pedidoService.getPedidosByClubPaginados(
                        seed.clubBId(), 0, 20, null, null, null));

        assertEquals("No tienes permisos para acceder a este club.", ex.getMessage());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void anfitrionACambiaEstadoPedidoClubPropioOk() {
        AccessSeed seed = seedAccess();
        authenticate(seed.hostAEmail());

        PedidoDTO dto = pedidoService.actualizarEstado(seed.pedidoAId(), "PREPARANDO", 10);

        assertEquals("PREPARANDO", dto.getEstado());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void anfitrionACambiaEstadoPedidoClubAjenoForbidden() {
        AccessSeed seed = seedAccess();
        authenticate(seed.hostAEmail());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> pedidoService.actualizarEstado(seed.pedidoBId(), "PREPARANDO", 10));

        assertEquals("No tienes permisos para acceder a este pedido.", ex.getMessage());
        assertEquals(EstadoPedido.RECIBIDO, estadoPedido(seed.pedidoBId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void usuarioBasicoNoAccedePedidosAjenos() {
        AccessSeed seed = seedAccess();
        authenticate(seed.basicoEmail());

        assertThrows(AccessDeniedException.class,
                () -> pedidoService.getPedidosBySocio(seed.membresiaAId()));
        assertThrows(AccessDeniedException.class,
                () -> pedidoService.getPedido(seed.pedidoAId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void regresionCreacionPropiaSigueFuncionando() {
        AccessSeed seed = seedAccess();
        authenticate(seed.socioAEmail());
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        PedidoDTO creado = tx.execute(status -> pedidoService.createPedidoConItems(
                requestItem(seed.productoAId()),
                seed.membresiaAId(),
                seed.clubAId()));

        assertNotNull(creado.getId());
    }

    private EstadoPedido estadoPedido(Integer pedidoId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> pedidoRepository.findById(pedidoId).orElseThrow().getEstado());
    }

    private static PedidoConItemsDTO requestItem(Integer productoId) {
        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setTipoConsumo("EN_LUGAR");
        PedidoItemDTO item = new PedidoItemDTO();
        item.setProductoId(productoId);
        item.setCantidad(1);
        request.setItems(List.of(item));
        return request;
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "n/a", Collections.emptyList()));
    }

    private AccessSeed seedAccess() {
        int n = SEQ.incrementAndGet();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rolSocio = rolRepository.save(rol("SOCIO"));
            Rol rolHost = rolRepository.save(rol("ANFITRION"));
            Rol rolBasico = rolRepository.save(rol("USUARIO_BASICO"));

            Usuario hostA = usuarioRepository.save(usuario(rolHost, "acc-host-a-" + n + "@test.com"));
            Usuario hostB = usuarioRepository.save(usuario(rolHost, "acc-host-b-" + n + "@test.com"));
            Usuario socioA = usuarioRepository.save(usuario(rolSocio, "acc-socio-a-" + n + "@test.com"));
            Usuario socioB = usuarioRepository.save(usuario(rolSocio, "acc-socio-b-" + n + "@test.com"));
            Usuario basico = usuarioRepository.save(usuario(rolBasico, "acc-basico-" + n + "@test.com"));

            Hub hubA = hubRepository.save(hub(hostA, n));
            Hub hubB = hubRepository.save(hub(hostB, n + 1000));
            Club clubA = clubRepository.save(club(hubA, hostA, n));
            Club clubB = clubRepository.save(club(hubB, hostB, n + 1000));

            Membresia membresiaA = membresiaRepository.save(membresia(socioA, clubA, "MA-" + n));
            Membresia membresiaB = membresiaRepository.save(membresia(socioB, clubB, "MB-" + n));

            Producto productoA = saveProducto(clubA, hubA, "Té A");
            Producto productoB = saveProducto(clubB, hubB, "Té B");

            Integer pedidoA = savePedido(membresiaA, clubA, productoA);
            Integer pedidoB = savePedido(membresiaB, clubB, productoB);

            return new AccessSeed(
                    clubA.getId(),
                    clubB.getId(),
                    membresiaA.getId(),
                    membresiaB.getId(),
                    productoA.getId(),
                    pedidoA,
                    pedidoB,
                    socioA.getEmail(),
                    socioB.getEmail(),
                    hostA.getEmail(),
                    hostB.getEmail(),
                    basico.getEmail());
        });
    }

    private Producto saveProducto(Club club, Hub hub, String nombre) {
        Producto producto = new Producto();
        producto.setHub(hub);
        producto.setClubCreador(club);
        producto.setNombre(nombre);
        producto.setPrecio(BigDecimal.TEN);
        producto.setTipo("LOCAL");
        producto.setEstadoAprobacion("APROBADO");
        producto.setActivo(true);
        producto = productoRepository.save(producto);
        var cp = new com.example.herbalife_clubes.entities.ClubProducto();
        cp.setClub(club);
        cp.setProducto(producto);
        cp.setDisponible(true);
        clubProductoRepository.save(cp);
        return producto;
    }

    private Integer savePedido(Membresia membresia, Club club, Producto producto) {
        Pedido pedido = new Pedido();
        pedido.setMembresia(membresia);
        pedido.setClub(club);
        pedido.setProducto(producto);
        pedido.setCantidad(1);
        pedido.setTipoConsumo(TipoConsumo.EN_LUGAR);
        pedido.setEstado(EstadoPedido.RECIBIDO);
        return pedidoRepository.save(pedido).getId();
    }

    private static Membresia membresia(Usuario socio, Club club, String numero) {
        Membresia m = new Membresia();
        m.setUsuario(socio);
        m.setClub(club);
        m.setNumeroSocio(numero);
        m.setEstado("ACTIVA");
        return m;
    }

    private static Rol rol(String nombre) {
        Rol r = new Rol();
        r.setNombre(nombre);
        return r;
    }

    private static Usuario usuario(Rol rol, String email) {
        Usuario u = new Usuario();
        u.setRol(rol);
        u.setNombre("T");
        u.setApellido("U");
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setEstado("ACTIVO");
        return u;
    }

    private static Hub hub(Usuario admin, int n) {
        Hub h = new Hub();
        h.setAdmin(admin);
        h.setNombre("Hub ACC-" + n);
        h.setEstado("ACTIVO");
        return h;
    }

    private static Club club(Hub hub, Usuario host, int n) {
        Club c = new Club();
        c.setHub(hub);
        c.setAnfitrion(host);
        c.setNombreClub("Club ACC-" + n);
        c.setEstado("ACTIVO");
        c.setPrefijoSocio("A" + n);
        return c;
    }

    private record AccessSeed(
            Integer clubAId,
            Integer clubBId,
            Integer membresiaAId,
            Integer membresiaBId,
            Integer productoAId,
            Integer pedidoAId,
            Integer pedidoBId,
            String socioAEmail,
            String socioBEmail,
            String hostAEmail,
            String hostBEmail,
            String basicoEmail) {
    }
}
