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
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.ProductoGrupoOpcion;
import com.example.herbalife_clubes.entities.ProductoOpcion;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ConflictException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
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
 * ORD-SYNC-001: idempotencia de POST /pedidos/con-items vía clientOrderId.
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
class PedidoClientOrderIdPersistenceTest {

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
    void primerRequestConUuidLowercaseCreaPedido() {
        Seed seed = seedSimple();
        String clientOrderId = "550e8400-e29b-41d4-a716-446655440000";
        PedidoDTO creado = crearConClientOrderId(seed, clientOrderId, requestItemSimple(seed));

        assertNotNull(creado.getId());
        assertEquals(clientOrderId, findClientOrderId(creado.getId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void retryConMismoUuidUppercaseRetornaMismoPedido() {
        Seed seed = seedSimple();
        String lowercase = "550e8400-e29b-41d4-a716-446655440001";
        String uppercase = "550E8400-E29B-41D4-A716-446655440001";
        PedidoConItemsDTO request = requestItemSimple(seed);

        PedidoDTO primero = crearConClientOrderId(seed, lowercase, request);
        long countAntes = countPedidos();
        PedidoDTO segundo = crearConClientOrderId(seed, uppercase, request);

        assertEquals(primero.getId(), segundo.getId());
        assertEquals(countAntes, countPedidos());
        assertEquals(lowercase, findClientOrderId(primero.getId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void uuidNoCanonicoRechazadoSinPersistir() {
        Seed seed = seedSimple();
        long countAntes = countPedidos();
        PedidoConItemsDTO request = requestItemSimple(seed);
        request.setClientOrderId("550e8400e29b41d4a716446655440002");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> crearPedido(seed, request));

        assertTrue(ex.getMessage().contains("clientOrderId"));
        assertEquals(countAntes, countPedidos());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void primerRequestConClientOrderIdCreaPedido() {
        Seed seed = seedSimple();
        String clientOrderId = newClientOrderId();
        PedidoDTO creado = crearConClientOrderId(seed, clientOrderId, requestItemSimple(seed));

        assertNotNull(creado.getId());
        assertEquals(clientOrderId, findClientOrderId(creado.getId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void segundoRequestMismoClientOrderIdRetornaMismoPedido() {
        Seed seed = seedSimple();
        String clientOrderId = newClientOrderId();
        PedidoConItemsDTO request = requestItemSimple(seed);

        PedidoDTO primero = crearConClientOrderId(seed, clientOrderId, request);
        long countAntes = countPedidos();
        PedidoDTO segundo = crearConClientOrderId(seed, clientOrderId, request);

        assertEquals(primero.getId(), segundo.getId());
        assertEquals(countAntes, countPedidos());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void retryNoDuplicaItemsCombosOpcionesNiNotificacion() {
        SeedCombo seed = seedComboModerno();
        String clientOrderId = newClientOrderId();
        PedidoConItemsDTO request = requestComboModerno(seed);

        PedidoDTO primero = crearConClientOrderId(seed, clientOrderId, request);
        long itemsAntes = countItemsPedido(primero.getId());
        long combosAntes = countCombosPedido(primero.getId());
        long opcionesAntes = countOpcionesPedido(primero.getId());
        long notifAntes = notificacionRepository.countByPedidoId(primero.getId());

        crearConClientOrderId(seed, clientOrderId, request);

        assertEquals(itemsAntes, countItemsPedido(primero.getId()));
        assertEquals(combosAntes, countCombosPedido(primero.getId()));
        assertEquals(opcionesAntes, countOpcionesPedido(primero.getId()));
        assertEquals(notifAntes, notificacionRepository.countByPedidoId(primero.getId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void clientOrderIdDistintoCreaSegundoPedido() {
        Seed seed = seedSimple();
        PedidoConItemsDTO request = requestItemSimple(seed);

        PedidoDTO uno = crearConClientOrderId(seed, newClientOrderId(), request);
        long countAntes = countPedidos();
        PedidoDTO dos = crearConClientOrderId(seed, newClientOrderId(), request);

        assertNotEquals(uno.getId(), dos.getId());
        assertEquals(countAntes + 1, countPedidos());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void mismoClientOrderIdOtraMembresiaRechazado() {
        Seed seed1 = seedSimple();
        Seed seed2 = seedSegundaMembresia(seed1.clubId());
        String clientOrderId = newClientOrderId();
        PedidoConItemsDTO request = requestItemSimple(seed1);

        crearConClientOrderId(seed1, clientOrderId, request);
        long countAntes = countPedidos();

        ConflictException ex = assertThrows(ConflictException.class,
                () -> crearConClientOrderIdAs(seed2, clientOrderId, request, seed2.socioEmail()));

        assertTrue(ex.getMessage().contains("clientOrderId"));
        assertEquals(countAntes, countPedidos());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void legacyNullPermiteDosPedidosIndependientes() {
        Seed seed = seedSimple();
        PedidoConItemsDTO request = requestItemSimple(seed);
        long countAntes = countPedidos();

        PedidoDTO uno = crearSinClientOrderId(seed, request);
        PedidoDTO dos = crearSinClientOrderId(seed, request);

        assertNotEquals(uno.getId(), dos.getId());
        assertEquals(countAntes + 2, countPedidos());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void clientOrderIdInvalidoRechazadoSinPersistir() {
        Seed seed = seedSimple();
        long countAntes = countPedidos();
        PedidoConItemsDTO request = requestItemSimple(seed);
        request.setClientOrderId("no-es-un-uuid");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> crearPedido(seed, request));

        assertTrue(ex.getMessage().contains("clientOrderId"));
        assertEquals(countAntes, countPedidos());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void pedidoSoloItemsRetryUnPedido() {
        Seed seed = seedSimple();
        String clientOrderId = newClientOrderId();
        PedidoConItemsDTO request = requestItemSimple(seed);

        PedidoDTO primero = crearConClientOrderId(seed, clientOrderId, request);
        PedidoDTO segundo = crearConClientOrderId(seed, clientOrderId, request);

        assertEquals(primero.getId(), segundo.getId());
        assertEquals(1, countItemsPedido(primero.getId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void pedidoComboModernoConOpcionesRetryUnPedido() {
        SeedCombo seed = seedComboModerno();
        String clientOrderId = newClientOrderId();
        PedidoConItemsDTO request = requestComboModerno(seed);

        PedidoDTO primero = crearConClientOrderId(seed, clientOrderId, request);
        PedidoDTO segundo = crearConClientOrderId(seed, clientOrderId, request);

        assertEquals(primero.getId(), segundo.getId());
        assertTrue(countCombosPedido(primero.getId()) >= 1);
        assertTrue(countOpcionesPedido(primero.getId()) >= 2);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    @Sql(scripts = "/db/test/pedidos_client_order_id_index.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void uniqueConstraintPostgreSQLImpideSegundoPedidoConMismoClientOrderId() {
        Seed seed = seedSimple();
        String clientOrderId = newClientOrderId();
        PedidoDTO creado = crearConClientOrderId(seed, clientOrderId, requestItemSimple(seed));

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThrows(DataIntegrityViolationException.class, () -> tx.execute(status -> {
            Pedido dup = new Pedido();
            dup.setMembresia(membresiaRepository.findById(seed.membresiaId()).orElseThrow());
            dup.setClub(clubRepository.findById(seed.clubId()).orElseThrow());
            dup.setProducto(productoRepository.findById(seed.productoId()).orElseThrow());
            dup.setCantidad(1);
            dup.setTipoConsumo(com.example.herbalife_clubes.entities.TipoConsumo.EN_LUGAR);
            dup.setEstado(com.example.herbalife_clubes.entities.EstadoPedido.RECIBIDO);
            dup.setClientOrderId(clientOrderId);
            pedidoRepository.saveAndFlush(dup);
            return null;
        }));

        assertEquals(creado.getId(), pedidoRepository.findByClientOrderId(clientOrderId).orElseThrow().getId());
    }

    private static String newClientOrderId() {
        return UUID.randomUUID().toString();
    }

    private PedidoDTO crearConClientOrderId(SeedBase seed, String clientOrderId, PedidoConItemsDTO request) {
        return crearConClientOrderIdAs(seed, clientOrderId, request, seed.socioEmail());
    }

    private PedidoDTO crearConClientOrderIdAs(SeedBase seed, String clientOrderId, PedidoConItemsDTO request, String email) {
        request.setClientOrderId(clientOrderId);
        return crearPedidoAs(seed, request, email);
    }

    private PedidoDTO crearSinClientOrderId(Seed seed, PedidoConItemsDTO request) {
        request.setClientOrderId(null);
        return crearPedido(seed, request);
    }

    private PedidoDTO crearPedido(SeedBase seed, PedidoConItemsDTO request) {
        return crearPedidoAs(seed, request, seed.socioEmail());
    }

    private PedidoDTO crearPedidoAs(SeedBase seed, PedidoConItemsDTO request, String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "n/a", Collections.emptyList()));
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> pedidoService.createPedidoConItems(
                request, seed.membresiaId(), seed.clubId()));
    }

    private static PedidoConItemsDTO requestItemSimple(Seed seed) {
        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setTipoConsumo("EN_LUGAR");
        PedidoItemDTO item = new PedidoItemDTO();
        item.setProductoId(seed.productoId());
        item.setCantidad(1);
        request.setItems(List.of(item));
        return request;
    }

    private static PedidoConItemsDTO requestComboModerno(SeedCombo seed) {
        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setTipoConsumo("EN_LUGAR");
        request.setItems(List.of());

        PedidoComboRequestDTO combo = new PedidoComboRequestDTO();
        combo.setComboId(seed.comboId());
        combo.setCantidad(1);
        combo.setComponentes(List.of(
                componente(seed.teId(), List.of()),
                componente(seed.aloeId(), List.of()),
                componente(seed.batidoId(), List.of(
                        sel(seed.grupoSaboresId(), seed.opcionFrutillaId(), 1),
                        sel(seed.grupoConsistenciaId(), seed.opcionCremosoId(), 1)))));

        request.setCombos(List.of(combo));
        return request;
    }

    private long countPedidos() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> pedidoRepository.count());
    }

    private String findClientOrderId(Integer pedidoId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> pedidoRepository.findById(pedidoId).orElseThrow().getClientOrderId());
    }

    private long countItemsPedido(Integer pedidoId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> (long) pedidoItemRepository.findByPedidoId(pedidoId).size());
    }

    private long countCombosPedido(Integer pedidoId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> pedidoComboRepository.findAll().stream()
                .filter(pc -> pc.getPedido() != null && pedidoId.equals(pc.getPedido().getId()))
                .count());
    }

    private long countOpcionesPedido(Integer pedidoId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Number n = (Number) entityManager.createNativeQuery("""
                    SELECT COUNT(*) FROM pedido_item_opciones pio
                    JOIN pedido_items pi ON pi.id = pio.pedido_item_id
                    WHERE pi.pedido_id = :pid
                    """)
                    .setParameter("pid", pedidoId)
                    .getSingleResult();
            return n.longValue();
        });
    }

    private Seed seedSimple() {
        int n = SEQ.incrementAndGet();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rol = rolRepository.save(rol("SOCIO"));
            Usuario socio = usuarioRepository.save(usuario(rol, "sync-socio-" + n + "@test.com"));
            Rol rolHost = rolRepository.save(rol("ANFITRION"));
            Usuario host = usuarioRepository.save(usuario(rolHost, "sync-host-" + n + "@test.com"));
            Hub hub = hubRepository.save(hub(host, n));
            Club club = clubRepository.save(club(hub, host, n));

            Membresia membresia = new Membresia();
            membresia.setUsuario(socio);
            membresia.setClub(club);
            membresia.setNumeroSocio("S-" + n);
            membresia.setEstado("ACTIVA");
            membresia = membresiaRepository.save(membresia);

            Producto producto = saveProducto(club, hub, "Té", BigDecimal.valueOf(15), false);
            return new Seed(membresia.getId(), club.getId(), producto.getId(), socio.getEmail());
        });
    }

    private Seed seedSegundaMembresia(Integer clubId) {
        int n = SEQ.incrementAndGet();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Club club = clubRepository.findById(clubId).orElseThrow();
            Hub hub = club.getHub();
            Rol rol = rolRepository.save(rol("SOCIO"));
            Usuario socio = usuarioRepository.save(usuario(rol, "sync-socio2-" + n + "@test.com"));

            Membresia membresia = new Membresia();
            membresia.setUsuario(socio);
            membresia.setClub(club);
            membresia.setNumeroSocio("S2-" + n);
            membresia.setEstado("ACTIVA");
            membresia = membresiaRepository.save(membresia);

            Producto producto = productoRepository.findAll().stream()
                    .filter(p -> club.getId().equals(p.getClubCreador().getId()))
                    .findFirst()
                    .orElseThrow();
            return new Seed(membresia.getId(), clubId, producto.getId(), socio.getEmail());
        });
    }

    private SeedCombo seedComboModerno() {
        int n = SEQ.incrementAndGet();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rol = rolRepository.save(rol("SOCIO"));
            Usuario socio = usuarioRepository.save(usuario(rol, "sync-combo-socio-" + n + "@test.com"));
            Rol rolHost = rolRepository.save(rol("ANFITRION"));
            Usuario host = usuarioRepository.save(usuario(rolHost, "sync-combo-host-" + n + "@test.com"));
            Hub hub = hubRepository.save(hub(host, n));
            Club club = clubRepository.save(club(hub, host, n));

            Membresia membresia = new Membresia();
            membresia.setUsuario(socio);
            membresia.setClub(club);
            membresia.setNumeroSocio("SC-" + n);
            membresia.setEstado("ACTIVA");
            membresia = membresiaRepository.save(membresia);

            Producto te = saveProducto(club, hub, "Té", BigDecimal.valueOf(15), false);
            Producto aloe = saveProducto(club, hub, "Aloe", BigDecimal.TEN, false);
            Producto batido = saveProducto(club, hub, "Batido", BigDecimal.valueOf(20), true);

            ProductoGrupoOpcion consistencia = new ProductoGrupoOpcion();
            consistencia.setProducto(batido);
            consistencia.setNombre("Consistencia");
            consistencia.setOrden(1);
            consistencia.setMinSelecciones(1);
            consistencia.setMaxSelecciones(1);
            consistencia.setPermiteRepetir(false);
            consistencia.setOpciones(new ArrayList<>());
            ProductoOpcion cremoso = opcionEntity(consistencia, "Cremoso", 0);
            consistencia.getOpciones().add(cremoso);
            batido.getGruposOpciones().add(consistencia);
            batido = productoRepository.saveAndFlush(batido);
            batido = reloadProducto(batido.getId());

            ProductoGrupoOpcion sabores = findGrupo(batido, "Sabores");
            ProductoGrupoOpcion consistenciaManaged = findGrupo(batido, "Consistencia");
            ProductoOpcion frutilla = findOpcion(sabores, "Frutilla");
            ProductoOpcion cremosoManaged = findOpcion(consistenciaManaged, "Cremoso");

            Combo combo = new Combo();
            combo.setClub(club);
            combo.setNombre("Combo sync");
            combo.setPrecio(new BigDecimal("38.00"));
            combo.setActivo(true);
            combo.setPuntosValor(15);
            combo.setItems(new ArrayList<>());
            combo.getItems().add(comboItem(combo, te, 1));
            combo.getItems().add(comboItem(combo, aloe, 1));
            combo.getItems().add(comboItem(combo, batido, 1));
            combo = comboRepository.saveAndFlush(combo);

            return new SeedCombo(
                    membresia.getId(),
                    club.getId(),
                    combo.getId(),
                    te.getId(),
                    aloe.getId(),
                    batido.getId(),
                    sabores.getId(),
                    frutilla.getId(),
                    consistenciaManaged.getId(),
                    cremosoManaged.getId(),
                    socio.getEmail());
        });
    }

    private Producto reloadProducto(Integer productoId) {
        entityManager.flush();
        entityManager.clear();
        return productoRepository.findById(productoId).orElseThrow();
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
            grupo.getOpciones().add(opcionEntity(grupo, "Frutilla", 0));
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

    private static ProductoGrupoOpcion findGrupo(Producto producto, String nombre) {
        return producto.getGruposOpciones().stream()
                .filter(g -> nombre.equals(g.getNombre()))
                .findFirst()
                .orElseThrow();
    }

    private static ProductoOpcion findOpcion(ProductoGrupoOpcion grupo, String nombre) {
        return grupo.getOpciones().stream()
                .filter(o -> nombre.equals(o.getNombre()))
                .findFirst()
                .orElseThrow();
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
        h.setNombre("Hub S-" + n);
        h.setEstado("ACTIVO");
        return h;
    }

    private static Club club(Hub hub, Usuario host, int n) {
        Club c = new Club();
        c.setHub(hub);
        c.setAnfitrion(host);
        c.setNombreClub("Club S-" + n);
        c.setEstado("ACTIVO");
        c.setPrefijoSocio("S" + n);
        return c;
    }

    private static ProductoOpcion opcionEntity(ProductoGrupoOpcion grupo, String nombre, int orden) {
        ProductoOpcion o = new ProductoOpcion();
        o.setGrupo(grupo);
        o.setNombre(nombre);
        o.setOrden(orden);
        return o;
    }

    private interface SeedBase {
        Integer membresiaId();
        Integer clubId();
        String socioEmail();
    }

    private record Seed(Integer membresiaId, Integer clubId, Integer productoId, String socioEmail) implements SeedBase {
    }

    private record SeedCombo(
            Integer membresiaId,
            Integer clubId,
            Integer comboId,
            Integer teId,
            Integer aloeId,
            Integer batidoId,
            Integer grupoSaboresId,
            Integer opcionFrutillaId,
            Integer grupoConsistenciaId,
            Integer opcionCremosoId,
            String socioEmail) implements SeedBase {
    }
}
