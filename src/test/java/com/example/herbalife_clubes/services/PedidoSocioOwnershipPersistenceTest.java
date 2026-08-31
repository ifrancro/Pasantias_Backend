package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.pedido.PedidoComboComponenteRequestDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoComboRequestDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoConItemsDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemOpcionResponseDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Combo;
import com.example.herbalife_clubes.entities.ComboItem;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.ProductoGrupoOpcion;
import com.example.herbalife_clubes.entities.ProductoOpcion;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.OrderCreationRejectedException;
import com.example.herbalife_clubes.pedidos.OrderCreationRejections;
import com.example.herbalife_clubes.pedidos.PedidoComboSupport;
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.ComboRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.NotificacionRepository;
import com.example.herbalife_clubes.repositories.PedidoComboRepository;
import com.example.herbalife_clubes.repositories.PedidoItemRepository;
import com.example.herbalife_clubes.repositories.PedidoRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.PedidoServiceImpl;
import jakarta.persistence.EntityManager;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SEC-ORDER-OWNERSHIP-001: pedidos socio solo con membresía propia; idempotencia sin filtrar datos ajenos.
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
class PedidoSocioOwnershipPersistenceTest {

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
    @Autowired private PedidoItemRepository pedidoItemRepository;
    @Autowired private PedidoComboRepository pedidoComboRepository;
    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private MembresiaRepository membresiaRepository;
    @Autowired private ClubRepository clubRepository;
    @Autowired private HubRepository hubRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ComboRepository comboRepository;
    @Autowired private ClubProductoRepository clubProductoRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void socioCreaPedidoConSuMembresia() {
        TwoSocioSeed seed = seedTwoSociosSameClub();
        PedidoDTO creado = crearItems(seed.socioA(), requestItemSimple(seed.socioA()));

        assertNotNull(creado.getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void socioNoPuedeUsarMembresiaAjena() {
        TwoSocioSeed seed = seedTwoSociosSameClub();
        long countAntes = countPedidos();

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> crearItemsAs(seed.socioB(), requestItemSimple(seed.socioB()), seed.socioA().email()));

        assertEquals("No tienes permisos para usar esta membresía.", ex.getMessage());
        assertEquals(countAntes, countPedidos());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void socioNoPuedeUsarMembresiaAjenaConCombos() {
        TwoSocioSeed seed = seedTwoSociosSameClub();
        long pedidosAntes = countPedidos();
        long itemsAntes = pedidoItemRepository.count();
        long combosAntes = pedidoComboRepository.count();
        long opcionesAntes = countAllOpciones();
        long notifAntes = notificacionRepository.count();

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> crearItemsAs(seed.socioB(), requestCombo(seed.comboSeed()), seed.socioA().email()));

        assertEquals("No tienes permisos para usar esta membresía.", ex.getMessage());
        assertEquals(pedidosAntes, countPedidos());
        assertEquals(itemsAntes, pedidoItemRepository.count());
        assertEquals(combosAntes, pedidoComboRepository.count());
        assertEquals(opcionesAntes, countAllOpciones());
        assertEquals(notifAntes, notificacionRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void idempotenciaSocioBCannotReuseClientOrderIdOfSocioA() {
        TwoSocioSeed seed = seedTwoSociosSameClub();
        String clientOrderId = UUID.randomUUID().toString();
        PedidoConItemsDTO request = requestItemSimple(seed.socioA());

        PedidoDTO deA = crearConClientOrderId(seed.socioA(), clientOrderId, request);
        long countAntes = countPedidos();

        OrderCreationRejectedException ex = assertThrows(OrderCreationRejectedException.class,
                () -> crearConClientOrderIdAs(seed.socioB(), clientOrderId, requestItemSimple(seed.socioB()), seed.socioB().email()));

        assertEquals(OrderCreationRejections.ORDER_CLIENT_ID_CONFLICT, ex.getErrorCode());

        assertEquals(countAntes, countPedidos());
        assertEquals(deA.getId(), pedidoRepository.findByClientOrderId(clientOrderId).orElseThrow().getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void idempotenciaRetryLegitimoDevuelveMismoPedido() {
        TwoSocioSeed seed = seedTwoSociosSameClub();
        String clientOrderId = UUID.randomUUID().toString();
        PedidoConItemsDTO request = requestItemSimple(seed.socioA());

        PedidoDTO primero = crearConClientOrderId(seed.socioA(), clientOrderId, request);
        long countAntes = countPedidos();
        PedidoDTO segundo = crearConClientOrderId(seed.socioA(), clientOrderId, request);

        assertEquals(primero.getId(), segundo.getId());
        assertEquals(countAntes, countPedidos());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void legacySinClientOrderIdConMembresiaPropia() {
        TwoSocioSeed seed = seedTwoSociosSameClub();
        long countAntes = countPedidos();

        PedidoDTO creado = crearItems(seed.socioA(), requestItemSimple(seed.socioA()));

        assertNotNull(creado.getId());
        assertEquals(countAntes + 1, countPedidos());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void createPedidoLegacyRequiereMembresiaPropia() {
        TwoSocioSeed seed = seedTwoSociosSameClub();
        long countAntes = countPedidos();

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> {
            authenticate(seed.socioA().email());
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.execute(status -> pedidoService.createPedido(
                    new PedidoDTO(), seed.socioB().membresiaId(), seed.socioB().clubId(), seed.socioB().productoId()));
        });

        assertEquals("No tienes permisos para usar esta membresía.", ex.getMessage());
        assertEquals(countAntes, countPedidos());
    }

    private PedidoDTO crearItems(SocioSeed socio, PedidoConItemsDTO request) {
        return crearItemsAs(socio, request, socio.email());
    }

    private PedidoDTO crearItemsAs(SocioSeed socio, PedidoConItemsDTO request, String email) {
        authenticate(email);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> pedidoService.createPedidoConItems(
                request, socio.membresiaId(), socio.clubId()));
    }

    private PedidoDTO crearConClientOrderId(SocioSeed socio, String clientOrderId, PedidoConItemsDTO request) {
        request.setClientOrderId(clientOrderId);
        return crearItems(socio, request);
    }

    private PedidoDTO crearConClientOrderIdAs(SocioSeed socio, String clientOrderId, PedidoConItemsDTO request, String email) {
        request.setClientOrderId(clientOrderId);
        return crearItemsAs(socio, request, email);
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "n/a", Collections.emptyList()));
    }

    private long countPedidos() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> pedidoRepository.count());
    }

    private long countAllOpciones() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Number n = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM pedido_item_opciones")
                    .getSingleResult();
            return n.longValue();
        });
    }

    private static PedidoConItemsDTO requestItemSimple(SocioSeed socio) {
        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setTipoConsumo("EN_LUGAR");
        PedidoItemDTO item = new PedidoItemDTO();
        item.setProductoId(socio.productoId());
        item.setCantidad(1);
        request.setItems(List.of(item));
        return request;
    }

    private PedidoConItemsDTO requestCombo(ComboSeed combo) {
        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setTipoConsumo("EN_LUGAR");
        request.setItems(List.of());
        PedidoComboRequestDTO comboReq = new PedidoComboRequestDTO();
        comboReq.setComboId(combo.comboId());
        comboReq.setCantidad(1);
        comboReq.setComponentes(List.of(
                componente(combo.batidoId(), List.of(
                        sel(combo.grupoId(), combo.opcionId(), 1)))));
        request.setCombos(List.of(comboReq));
        return request;
    }

    private TwoSocioSeed seedTwoSociosSameClub() {
        int n = SEQ.incrementAndGet();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rolSocio = rolRepository.save(rol("SOCIO"));
            Rol rolHost = rolRepository.save(rol("ANFITRION"));
            Usuario host = usuarioRepository.save(usuario(rolHost, "own-host-" + n + "@test.com"));
            Hub hub = hubRepository.save(hub(host, n));
            Club club = clubRepository.save(club(hub, host, n));

            Usuario socioA = usuarioRepository.save(usuario(rolSocio, "own-a-" + n + "@test.com"));
            Membresia membresiaA = membresia(socioA, club, "OA-" + n);
            membresiaA = membresiaRepository.save(membresiaA);

            Usuario socioB = usuarioRepository.save(usuario(rolSocio, "own-b-" + n + "@test.com"));
            Membresia membresiaB = membresia(socioB, club, "OB-" + n);
            membresiaB = membresiaRepository.save(membresiaB);

            Producto producto = saveProducto(club, hub, "Té", BigDecimal.valueOf(15), false);
            Producto batido = saveProducto(club, hub, "Batido", BigDecimal.valueOf(20), true);
            batido = productoRepository.saveAndFlush(batido);
            entityManager.flush();
            entityManager.clear();
            batido = productoRepository.findById(batido.getId()).orElseThrow();
            ProductoGrupoOpcion grupo = batido.getGruposOpciones().get(0);
            ProductoOpcion opcion = grupo.getOpciones().get(0);

            Combo combo = new Combo();
            combo.setClub(club);
            combo.setNombre("Combo own");
            combo.setPrecio(new BigDecimal("30.00"));
            combo.setActivo(true);
            combo.setPuntosValor(10);
            combo.setItems(new ArrayList<>());
            combo.getItems().add(comboItem(combo, batido, 1));
            combo = comboRepository.save(combo);

            SocioSeed seedA = new SocioSeed(
                    membresiaA.getId(), club.getId(), producto.getId(), socioA.getEmail());
            SocioSeed seedB = new SocioSeed(
                    membresiaB.getId(), club.getId(), producto.getId(), socioB.getEmail());
            ComboSeed comboSeed = new ComboSeed(
                    membresiaB.getId(), club.getId(), combo.getId(), batido.getId(),
                    grupo.getId(), opcion.getId(), socioB.getEmail());
            return new TwoSocioSeed(seedA, seedB, comboSeed);
        });
    }

    private Producto saveProducto(Club club, Hub hub, String nombre, BigDecimal precio, boolean conGrupos) {
        Producto producto = new Producto();
        producto.setHub(hub);
        producto.setClubCreador(club);
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setTipo("LOCAL");
        producto.setEstadoAprobacion("APROBADO");
        producto.setActivo(true);
        producto.setGruposOpciones(new ArrayList<>());
        if (conGrupos) {
            ProductoGrupoOpcion grupo = new ProductoGrupoOpcion();
            grupo.setProducto(producto);
            grupo.setNombre("Sabores");
            grupo.setOrden(0);
            grupo.setMinSelecciones(1);
            grupo.setMaxSelecciones(1);
            grupo.setPermiteRepetir(false);
            grupo.setOpciones(new ArrayList<>());
            ProductoOpcion frutilla = new ProductoOpcion();
            frutilla.setGrupo(grupo);
            frutilla.setNombre("Frutilla");
            frutilla.setOrden(0);
            grupo.getOpciones().add(frutilla);
            producto.getGruposOpciones().add(grupo);
        }
        producto = productoRepository.saveAndFlush(producto);
        var cp = new com.example.herbalife_clubes.entities.ClubProducto();
        cp.setClub(club);
        cp.setProducto(producto);
        cp.setDisponible(true);
        clubProductoRepository.save(cp);
        return producto;
    }

    private static Membresia membresia(Usuario socio, Club club, String numero) {
        Membresia m = new Membresia();
        m.setUsuario(socio);
        m.setClub(club);
        m.setNumeroSocio(numero);
        m.setEstado("ACTIVA");
        return m;
    }

    private static ComboItem comboItem(Combo combo, Producto producto, int cantidad) {
        ComboItem item = new ComboItem();
        item.setCombo(combo);
        item.setProducto(producto);
        item.setCantidad(cantidad);
        return item;
    }

    private static PedidoComboComponenteRequestDTO componente(int productoId, List<PedidoItemOpcionResponseDTO> opciones) {
        PedidoComboComponenteRequestDTO dto = new PedidoComboComponenteRequestDTO();
        dto.setProductoId(productoId);
        dto.setOpciones(opciones);
        return dto;
    }

    private static PedidoItemOpcionResponseDTO sel(int grupoId, int opcionId, int cantidad) {
        PedidoItemOpcionResponseDTO dto = new PedidoItemOpcionResponseDTO();
        dto.setGrupoId(grupoId);
        dto.setOpcionId(opcionId);
        dto.setCantidad(cantidad);
        return dto;
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
        h.setNombre("Hub O-" + n);
        h.setEstado("ACTIVO");
        return h;
    }

    private static Club club(Hub hub, Usuario host, int n) {
        Club c = new Club();
        c.setHub(hub);
        c.setAnfitrion(host);
        c.setNombreClub("Club O-" + n);
        c.setEstado("ACTIVO");
        c.setPrefijoSocio("O" + n);
        return c;
    }

    private record SocioSeed(Integer membresiaId, Integer clubId, Integer productoId, String email) {
    }

    private record ComboSeed(
            Integer membresiaId, Integer clubId, Integer comboId, Integer batidoId,
            Integer grupoId, Integer opcionId, String email) {
    }

    private record TwoSocioSeed(SocioSeed socioA, SocioSeed socioB, ComboSeed comboSeed) {
    }
}
