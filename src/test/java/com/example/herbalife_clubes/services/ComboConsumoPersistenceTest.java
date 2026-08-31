package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.EstadoPedido;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.PedidoCombo;
import com.example.herbalife_clubes.entities.PedidoItem;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.TipoConsumo;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ComboRequiredException;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.PedidoRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.ComboConsumoServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

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
@Import(ComboConsumoServiceImpl.class)
class ComboConsumoPersistenceTest {

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

    @Autowired private ComboConsumoService comboConsumoService;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private MembresiaRepository membresiaRepository;
    @Autowired private ClubRepository clubRepository;
    @Autowired private HubRepository hubRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void modernoV21EntregadoHoyCuentaComoComboConsumido() {
        Seed seed = seed();
        Integer membresiaId = persistModerno(seed, EstadoPedido.ENTREGADO, LocalDateTime.now(), 3, 1, false);

        assertTrue(comboConsumoService.obtenerEstadoCombo(membresiaId).isHaConsumidoCombo());
        assertDoesNotThrow(() -> comboConsumoService.validarComboConsumidoAntesDeAsistencia(membresiaId));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void modernoRecibidoNoCuenta() {
        Seed seed = seed();
        Integer membresiaId = persistModerno(seed, EstadoPedido.RECIBIDO, LocalDateTime.now(), 3, 1, false);
        assertFalse(comboConsumoService.obtenerEstadoCombo(membresiaId).isHaConsumidoCombo());
        assertThrows(ComboRequiredException.class,
                () -> comboConsumoService.validarComboConsumidoAntesDeAsistencia(membresiaId));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void modernoPreparandoNoCuenta() {
        Seed seed = seed();
        Integer membresiaId = persistModerno(seed, EstadoPedido.PREPARANDO, LocalDateTime.now(), 3, 1, false);
        assertFalse(comboConsumoService.obtenerEstadoCombo(membresiaId).isHaConsumidoCombo());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void modernoListoNoCuenta() {
        Seed seed = seed();
        Integer membresiaId = persistModerno(seed, EstadoPedido.LISTO, LocalDateTime.now(), 3, 1, false);
        assertFalse(comboConsumoService.obtenerEstadoCombo(membresiaId).isHaConsumidoCombo());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void modernoCanceladoNoCuenta() {
        Seed seed = seed();
        Integer membresiaId = persistModerno(seed, EstadoPedido.CANCELADO, LocalDateTime.now(), 3, 1, false);
        assertFalse(comboConsumoService.obtenerEstadoCombo(membresiaId).isHaConsumidoCombo());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void modernoEntregadoAyerNoCuenta() {
        Seed seed = seed();
        Integer membresiaId = persistModerno(
                seed, EstadoPedido.ENTREGADO, LocalDateTime.now().minusDays(1), 3, 1, false);
        assertFalse(comboConsumoService.obtenerEstadoCombo(membresiaId).isHaConsumidoCombo());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void soloStandaloneEntregadoHoyNoCuenta() {
        Seed seed = seed();
        Integer membresiaId = persistStandalone(seed, EstadoPedido.ENTREGADO, LocalDateTime.now());
        assertFalse(comboConsumoService.obtenerEstadoCombo(membresiaId).isHaConsumidoCombo());
        assertThrows(ComboRequiredException.class,
                () -> comboConsumoService.validarComboConsumidoAntesDeAsistencia(membresiaId));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void legacyComboEntregadoHoyCuenta() {
        Seed seed = seed();
        Integer membresiaId = persistLegacy(seed, EstadoPedido.ENTREGADO, LocalDateTime.now());
        assertTrue(comboConsumoService.obtenerEstadoCombo(membresiaId).isHaConsumidoCombo());
        assertDoesNotThrow(() -> comboConsumoService.validarComboConsumidoAntesDeAsistencia(membresiaId));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void pedidoMixtoStandaloneMasComboModernoEntregadoHoyCuenta() {
        Seed seed = seed();
        Integer membresiaId = persistModerno(seed, EstadoPedido.ENTREGADO, LocalDateTime.now(), 3, 1, true);
        assertTrue(comboConsumoService.obtenerEstadoCombo(membresiaId).isHaConsumidoCombo());
        assertDoesNotThrow(() -> comboConsumoService.validarComboConsumidoAntesDeAsistencia(membresiaId));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void conteoModernoTresComponentesCuentaUnComboNoTres() {
        Seed seed = seed();
        Integer membresiaId = persistModerno(seed, EstadoPedido.ENTREGADO, LocalDateTime.now(), 3, 1, false);
        assertEquals(1L, comboConsumoService.obtenerEstadoCombo(membresiaId).getTotalCombosConsumidos());
    }

    private Integer persistModerno(
            Seed seed,
            EstadoPedido estado,
            LocalDateTime fecha,
            int componentes,
            int cantidadCombo,
            boolean incluirStandalone) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Membresia membresia = membresiaRepository.findById(seed.membresiaId).orElseThrow();
            Club club = clubRepository.findById(seed.clubId).orElseThrow();
            Producto te = producto(seed.hub, club, "Té", false);
            Producto aloe = producto(seed.hub, club, "Aloe", false);
            Producto batido = producto(seed.hub, club, "Batido", false);
            te = productoRepository.save(te);
            aloe = productoRepository.save(aloe);
            batido = productoRepository.save(batido);

            List<Producto> componentProducts = List.of(te, aloe, batido).subList(0, componentes);

            Pedido pedido = basePedido(membresia, club, estado, fecha, te);

            PedidoCombo pedidoCombo = new PedidoCombo();
            pedidoCombo.setPedido(pedido);
            pedidoCombo.setComboNombreSnapshot("Combo desayuno");
            pedidoCombo.setCantidad(cantidadCombo);
            pedidoCombo.setPrecioUnitarioSnapshot(new BigDecimal("38.00"));
            pedidoCombo.setSubtotalSnapshot(new BigDecimal("38.00").multiply(BigDecimal.valueOf(cantidadCombo)));
            pedidoCombo.setPuntosValorSnapshot(15);

            for (Producto componentProduct : componentProducts) {
                PedidoItem componente = componenteCero(pedido, pedidoCombo, componentProduct);
                pedidoCombo.getItems().add(componente);
                pedido.getItems().add(componente);
            }
            pedido.getPedidoCombos().add(pedidoCombo);

            if (incluirStandalone) {
                Producto extra = productoRepository.save(producto(seed.hub, club, "Shake extra", false));
                PedidoItem suelto = itemConPrecio(pedido, extra, new BigDecimal("20.00"));
                pedido.getItems().add(suelto);
            }

            pedidoRepository.save(pedido);
            return membresia.getId();
        });
    }

    private Integer persistLegacy(Seed seed, EstadoPedido estado, LocalDateTime fecha) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Membresia membresia = membresiaRepository.findById(seed.membresiaId).orElseThrow();
            Club club = clubRepository.findById(seed.clubId).orElseThrow();
            Producto legacyCombo = producto(seed.hub, club, "Combo legacy", true);
            legacyCombo = productoRepository.save(legacyCombo);

            Pedido pedido = basePedido(membresia, club, estado, fecha, legacyCombo);
            PedidoItem item = itemConPrecio(pedido, legacyCombo, new BigDecimal("30.00"));
            pedido.getItems().add(item);
            pedidoRepository.save(pedido);
            return membresia.getId();
        });
    }

    private Integer persistStandalone(Seed seed, EstadoPedido estado, LocalDateTime fecha) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Membresia membresia = membresiaRepository.findById(seed.membresiaId).orElseThrow();
            Club club = clubRepository.findById(seed.clubId).orElseThrow();
            Producto shake = productoRepository.save(producto(seed.hub, club, "Shake", false));

            Pedido pedido = basePedido(membresia, club, estado, fecha, shake);
            pedido.getItems().add(itemConPrecio(pedido, shake, new BigDecimal("25.00")));
            pedidoRepository.save(pedido);
            return membresia.getId();
        });
    }

    private static Pedido basePedido(
            Membresia membresia, Club club, EstadoPedido estado, LocalDateTime fecha, Producto primerProducto) {
        Pedido pedido = new Pedido();
        pedido.setMembresia(membresia);
        pedido.setClub(club);
        pedido.setEstado(estado);
        pedido.setTipoConsumo(TipoConsumo.EN_LUGAR);
        pedido.setFechaPedido(fecha);
        pedido.setProducto(primerProducto);
        pedido.setCantidad(1);
        pedido.setItems(new ArrayList<>());
        pedido.setPedidoCombos(new ArrayList<>());
        return pedido;
    }

    private static PedidoItem componenteCero(Pedido pedido, PedidoCombo pedidoCombo, Producto producto) {
        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setPedidoCombo(pedidoCombo);
        item.setProducto(producto);
        item.setCantidad(1);
        item.setPrecioUnitario(BigDecimal.ZERO);
        item.setSubtotal(BigDecimal.ZERO);
        return item;
    }

    private static PedidoItem itemConPrecio(Pedido pedido, Producto producto, BigDecimal precio) {
        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setProducto(producto);
        item.setCantidad(1);
        item.setPrecioUnitario(precio);
        item.setSubtotal(precio);
        return item;
    }

    private Seed seed() {
        int n = SEQ.incrementAndGet();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rol = rolRepository.save(rol("SOCIO"));
            Usuario socio = usuarioRepository.save(usuario(rol, "combo-socio-" + n + "@test.com"));
            Rol rolHost = rolRepository.save(rol("ANFITRION"));
            Usuario host = usuarioRepository.save(usuario(rolHost, "combo-host-" + n + "@test.com"));
            Hub hub = hubRepository.save(hub(host, n));
            Club club = clubRepository.save(club(hub, host, n));

            Membresia membresia = new Membresia();
            membresia.setUsuario(socio);
            membresia.setClub(club);
            membresia.setNumeroSocio("C-" + n);
            membresia.setEstado("ACTIVA");
            membresia = membresiaRepository.save(membresia);
            return new Seed(membresia.getId(), club.getId(), hub);
        });
    }

    private static Producto producto(Hub hub, Club club, String nombre, boolean esComboLegacy) {
        Producto p = new Producto();
        p.setHub(hub);
        p.setClubCreador(club);
        p.setNombre(nombre);
        p.setPrecio(BigDecimal.TEN);
        p.setTipo(esComboLegacy ? "COMBO" : "LOCAL");
        p.setEsCombo(esComboLegacy);
        p.setEstadoAprobacion("APROBADO");
        p.setActivo(true);
        return p;
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
        h.setNombre("Hub CC-" + n);
        h.setEstado("ACTIVO");
        return h;
    }

    private static Club club(Hub hub, Usuario host, int n) {
        Club c = new Club();
        c.setHub(hub);
        c.setAnfitrion(host);
        c.setNombreClub("Club CC-" + n);
        c.setEstado("ACTIVO");
        c.setPrefijoSocio("C" + n);
        return c;
    }

    private record Seed(Integer membresiaId, Integer clubId, Hub hub) {
    }
}
