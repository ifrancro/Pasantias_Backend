package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.pedido.PedidoComboComponenteRequestDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoComboRequestDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoConItemsDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemOpcionResponseDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Combo;
import com.example.herbalife_clubes.entities.ComboItem;
import com.example.herbalife_clubes.entities.EstadoPedido;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.ProductoGrupoOpcion;
import com.example.herbalife_clubes.entities.ProductoOpcion;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.pedidos.PedidoComboSupport;
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.ComboRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regresión ORDER-STATUS-001: actualizarEstado/cancelarPedido deben mapear relaciones LAZY
 * (items, opciones, pedidoCombos) dentro de la misma transacción del servicio.
 * Con {@code spring.jpa.open-in-view=false}, sin @Transactional el mapper fallaba tras save().
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
class PedidoEstadoActualizacionPersistenceTest {

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
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ComboRepository comboRepository;
    @Autowired private MembresiaRepository membresiaRepository;
    @Autowired private ClubRepository clubRepository;
    @Autowired private HubRepository hubRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
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
    void casoA_productoSencilloRecibidoAPreparando() {
        Seed seed = seedBase();
        Integer pedidoId = crearPedidoSencillo(seed);
        authenticateHost(seed.clubId());

        PedidoDTO dto = pedidoService.actualizarEstado(pedidoId, "PREPARANDO", 8);

        assertEquals("PREPARANDO", dto.getEstado());
        assertEquals(8, dto.getTiempoEstimadoMinutos());
        assertFalse(dto.getItems().isEmpty());
        assertEstadoPersistido(pedidoId, EstadoPedido.PREPARANDO, 8);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void casoB_productoConOpcionesDevuelveOpcionesAlCambiarEstado() {
        Seed seed = seedBase();
        Integer pedidoId = crearPedidoConOpciones(seed);
        authenticateHost(seed.clubId());

        PedidoDTO dto = pedidoService.actualizarEstado(pedidoId, "PREPARANDO", 10);

        assertEquals("PREPARANDO", dto.getEstado());
        assertEquals(1, dto.getItems().size());
        assertEquals(1, dto.getItems().get(0).getOpciones().size());
        assertEquals("Frutilla", dto.getItems().get(0).getOpciones().get(0).getOpcionNombre());
        assertEstadoPersistido(pedidoId, EstadoPedido.PREPARANDO, 10);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void casoC_comboModernoRecibidoAPreparandoDevuelveCombosYOpciones() {
        SeedCombo seed = seedComboModerno();
        Integer pedidoId = crearPedidoComboModerno(seed);
        authenticateHost(seed.clubId());

        PedidoDTO dto = pedidoService.actualizarEstado(pedidoId, "PREPARANDO", 8);

        assertEquals("PREPARANDO", dto.getEstado());
        assertEquals(8, dto.getTiempoEstimadoMinutos());
        assertEquals(1, dto.getCombos().size());
        assertEquals(0, new BigDecimal("38.00").compareTo(dto.getCombos().get(0).getPrecioUnitario()));
        assertEquals(0, new BigDecimal("38.00").compareTo(dto.getCombos().get(0).getSubtotal()));
        assertEquals(3, dto.getCombos().get(0).getItems().size());

        var batido = dto.getCombos().get(0).getItems().stream()
                .filter(i -> Integer.valueOf(seed.batidoId).equals(i.getProductoId()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, batido.getOpciones().size());

        for (PedidoItemDTO itemPlano : dto.getItems()) {
            if (itemPlano.getPedidoComboId() != null) {
                assertEquals(0, BigDecimal.ZERO.compareTo(itemPlano.getSubtotal()));
                assertEquals(0, BigDecimal.ZERO.compareTo(itemPlano.getPrecioUnitario()));
            }
        }

        assertEstadoPersistido(pedidoId, EstadoPedido.PREPARANDO, 8);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void casoD_y_E_flujoPreparandoListoEntregado() {
        Seed seed = seedBase();
        Integer pedidoId = crearPedidoSencillo(seed);
        authenticateHost(seed.clubId());

        pedidoService.actualizarEstado(pedidoId, "PREPARANDO", 5);
        assertEstadoPersistido(pedidoId, EstadoPedido.PREPARANDO, 5);

        PedidoDTO listo = pedidoService.actualizarEstado(pedidoId, "LISTO", null);
        assertEquals("LISTO", listo.getEstado());
        assertEstadoPersistido(pedidoId, EstadoPedido.LISTO, 5);

        PedidoDTO entregado = pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);
        assertEquals("ENTREGADO", entregado.getEstado());
        assertEstadoPersistido(pedidoId, EstadoPedido.ENTREGADO, 5);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void cancelarPedidoComboModernoDevuelveDtoCompleto() {
        SeedCombo seed = seedComboModerno();
        Integer pedidoId = crearPedidoComboModerno(seed);
        authenticateSocio(seed.membresiaId());

        PedidoDTO dto = pedidoService.cancelarPedido(pedidoId);

        assertEquals("CANCELADO", dto.getEstado());
        assertEquals(1, dto.getCombos().size());
        assertEstadoPersistido(pedidoId, EstadoPedido.CANCELADO, null);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void actualizarEstadoPersisteSoloSiOperacionCompletaExitosa() {
        Seed seed = seedBase();
        Integer pedidoId = crearPedidoSencillo(seed);
        authenticateHost(seed.clubId());

        PedidoDTO dto = pedidoService.actualizarEstado(pedidoId, "PREPARANDO", 12);

        assertEquals("PREPARANDO", dto.getEstado());
        assertEstadoPersistido(pedidoId, EstadoPedido.PREPARANDO, 12);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void hostCancelaRecibido() {
        Seed seed = seedBase();
        Integer pedidoId = crearPedidoSencillo(seed);
        authenticateHost(seed.clubId());

        PedidoDTO dto = pedidoService.actualizarEstado(pedidoId, "CANCELADO", null);

        assertEquals("CANCELADO", dto.getEstado());
        assertEstadoPersistido(pedidoId, EstadoPedido.CANCELADO, null);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void hostCancelaPreparando() {
        Seed seed = seedBase();
        Integer pedidoId = crearPedidoSencillo(seed);
        authenticateHost(seed.clubId());
        pedidoService.actualizarEstado(pedidoId, "PREPARANDO", 10);

        PedidoDTO dto = pedidoService.actualizarEstado(pedidoId, "CANCELADO", null);

        assertEquals("CANCELADO", dto.getEstado());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void hostCancelaListo() {
        Seed seed = seedBase();
        Integer pedidoId = crearPedidoSencillo(seed);
        authenticateHost(seed.clubId());
        pedidoService.actualizarEstado(pedidoId, "PREPARANDO", 10);
        pedidoService.actualizarEstado(pedidoId, "LISTO", null);

        PedidoDTO dto = pedidoService.actualizarEstado(pedidoId, "CANCELADO", null);

        assertEquals("CANCELADO", dto.getEstado());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void entregadoNoPuedeCancelar() {
        Seed seed = seedBase();
        Integer pedidoId = crearPedidoSencillo(seed);
        authenticateHost(seed.clubId());
        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.actualizarEstado(pedidoId, "CANCELADO", null));

        assertTrue(ex.getMessage().contains("entregado"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void entregadoNoPuedeVolverAPreparando() {
        Seed seed = seedBase();
        Integer pedidoId = crearPedidoSencillo(seed);
        authenticateHost(seed.clubId());
        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);

        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.actualizarEstado(pedidoId, "PREPARANDO", 5));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void canceladoNoPuedeVolverARecibido() {
        Seed seed = seedBase();
        Integer pedidoId = crearPedidoSencillo(seed);
        authenticateHost(seed.clubId());
        pedidoService.actualizarEstado(pedidoId, "CANCELADO", null);

        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.actualizarEstado(pedidoId, "RECIBIDO", null));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void canceladoNoPuedeEntregar() {
        Seed seed = seedBase();
        Integer pedidoId = crearPedidoSencillo(seed);
        authenticateHost(seed.clubId());
        pedidoService.actualizarEstado(pedidoId, "CANCELADO", null);

        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void entregadoIdempotenteNoDuplicaPuntos() {
        Seed seed = seedBase();
        Integer pedidoId = crearPedidoSencillo(seed);
        authenticateHost(seed.clubId());
        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);

        PedidoDTO dto = pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);

        assertEquals("ENTREGADO", dto.getEstado());
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            entityManager.clear();
            Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
            assertTrue(Boolean.TRUE.equals(pedido.getPuntosAcreditados()));
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void canceladoIdempotentePermitido() {
        Seed seed = seedBase();
        Integer pedidoId = crearPedidoSencillo(seed);
        authenticateHost(seed.clubId());
        pedidoService.actualizarEstado(pedidoId, "CANCELADO", null);

        PedidoDTO dto = pedidoService.actualizarEstado(pedidoId, "CANCELADO", null);

        assertEquals("CANCELADO", dto.getEstado());
    }

    private Integer crearPedidoSencillo(Seed seed) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            authenticateSocio(seed.membresiaId());
            PedidoConItemsDTO request = new PedidoConItemsDTO();
            request.setTipoConsumo("EN_LUGAR");
            PedidoItemDTO item = new PedidoItemDTO();
            item.setProductoId(seed.teId);
            item.setCantidad(1);
            request.setItems(List.of(item));
            return pedidoService.createPedidoConItems(request, seed.membresiaId, seed.clubId).getId();
        });
    }

    private Integer crearPedidoConOpciones(Seed seed) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            authenticateSocio(seed.membresiaId());
            PedidoConItemsDTO request = new PedidoConItemsDTO();
            request.setTipoConsumo("EN_LUGAR");
            PedidoItemDTO item = new PedidoItemDTO();
            item.setProductoId(seed.batidoId);
            item.setCantidad(1);
            item.setOpciones(List.of(sel(seed.grupoId, seed.opcionId, 1)));
            request.setItems(List.of(item));
            return pedidoService.createPedidoConItems(request, seed.membresiaId, seed.clubId).getId();
        });
    }

    private Integer crearPedidoComboModerno(SeedCombo seed) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            authenticateSocio(seed.membresiaId());
            PedidoConItemsDTO request = new PedidoConItemsDTO();
            request.setTipoConsumo("EN_LUGAR");
            request.setItems(List.of());

            PedidoComboRequestDTO combo = new PedidoComboRequestDTO();
            combo.setComboId(seed.comboId);
            combo.setCantidad(1);
            combo.setComponentes(List.of(
                    componente(seed.teId, List.of()),
                    componente(seed.aloeId, List.of()),
                    componente(seed.batidoId, List.of(
                            sel(seed.grupoSaboresId, seed.opcionFrutillaId, 1),
                            sel(seed.grupoConsistenciaId, seed.opcionCremosoId, 1)))));

            request.setCombos(List.of(combo));
            return pedidoService.createPedidoConItems(request, seed.membresiaId, seed.clubId).getId();
        });
    }

    private void assertEstadoPersistido(Integer pedidoId, EstadoPedido estado, Integer tiempoEsperado) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            entityManager.clear();
            Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
            assertEquals(estado, pedido.getEstado());
            if (tiempoEsperado != null) {
                assertEquals(tiempoEsperado, pedido.getTiempoEstimadoMinutos());
            }
        });
    }

    private Seed seedBase() {
        int n = SEQ.incrementAndGet();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rol = rolRepository.save(rol("SOCIO"));
            Usuario socio = usuarioRepository.save(usuario(rol, "estado-socio-" + n + "@test.com"));
            Rol rolHost = rolRepository.save(rol("ANFITRION"));
            Usuario host = usuarioRepository.save(usuario(rolHost, "estado-host-" + n + "@test.com"));
            Hub hub = hubRepository.save(hub(host, n));
            Club club = clubRepository.save(club(hub, host, n));

            Membresia membresia = new Membresia();
            membresia.setUsuario(socio);
            membresia.setClub(club);
            membresia.setNumeroSocio("E-" + n);
            membresia.setEstado("ACTIVA");
            membresia = membresiaRepository.save(membresia);

            Producto te = saveProducto(club, hub, "Té", BigDecimal.valueOf(15), false);
            Producto batido = saveProducto(club, hub, "Batido", BigDecimal.valueOf(20), true);
            batido = reloadProductoConGrupos(batido.getId());

            ProductoGrupoOpcion sabores = findGrupo(batido, "Sabores");
            ProductoOpcion frutilla = findOpcion(sabores, "Frutilla");
            assertNotNull(sabores.getId());
            assertNotNull(frutilla.getId());

            return new Seed(
                    membresia.getId(),
                    club.getId(),
                    te.getId(),
                    batido.getId(),
                    sabores.getId(),
                    frutilla.getId());
        });
    }

    private SeedCombo seedComboModerno() {
        int n = SEQ.incrementAndGet();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rol = rolRepository.save(rol("SOCIO"));
            Usuario socio = usuarioRepository.save(usuario(rol, "estado-combo-socio-" + n + "@test.com"));
            Rol rolHost = rolRepository.save(rol("ANFITRION"));
            Usuario host = usuarioRepository.save(usuario(rolHost, "estado-combo-host-" + n + "@test.com"));
            Hub hub = hubRepository.save(hub(host, n));
            Club club = clubRepository.save(club(hub, host, n));

            Membresia membresia = new Membresia();
            membresia.setUsuario(socio);
            membresia.setClub(club);
            membresia.setNumeroSocio("EC-" + n);
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

            batido = reloadProductoConGrupos(batido.getId());
            ProductoGrupoOpcion sabores = findGrupo(batido, "Sabores");
            ProductoGrupoOpcion consistenciaManaged = findGrupo(batido, "Consistencia");
            ProductoOpcion frutilla = findOpcion(sabores, "Frutilla");
            ProductoOpcion cremosoManaged = findOpcion(consistenciaManaged, "Cremoso");

            assertNotNull(sabores.getId(), "grupoSaboresId");
            assertNotNull(frutilla.getId(), "opcionFrutillaId");
            assertNotNull(consistenciaManaged.getId(), "grupoConsistenciaId");
            assertNotNull(cremosoManaged.getId(), "opcionCremosoId");

            Combo combo = new Combo();
            combo.setClub(club);
            combo.setNombre("Combo desayuno");
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
                    cremosoManaged.getId());
        });
    }

    private Producto reloadProductoConGrupos(Integer productoId) {
        entityManager.flush();
        entityManager.clear();
        return productoRepository.findById(productoId).orElseThrow();
    }

    private static ProductoGrupoOpcion findGrupo(Producto producto, String nombre) {
        return producto.getGruposOpciones().stream()
                .filter(g -> nombre.equals(g.getNombre()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Grupo no encontrado: " + nombre));
    }

    private static ProductoOpcion findOpcion(ProductoGrupoOpcion grupo, String nombre) {
        return grupo.getOpciones().stream()
                .filter(o -> nombre.equals(o.getNombre()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Opción no encontrada: " + nombre));
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
            ProductoOpcion frutilla = opcionEntity(grupo, "Frutilla", 0);
            grupo.getOpciones().add(frutilla);
            producto.getGruposOpciones().add(grupo);
        }

        producto = productoRepository.saveAndFlush(producto);

        ClubProducto cp = new ClubProducto();
        cp.setClub(club);
        cp.setProducto(producto);
        cp.setDisponible(true);
        clubProductoRepository.save(cp);
        return producto;
    }

    private static ComboItem comboItem(Combo combo, Producto producto, int cantidad) {
        ComboItem item = new ComboItem();
        item.setCombo(combo);
        item.setProducto(producto);
        item.setCantidad(cantidad);
        return item;
    }

    private static PedidoComboComponenteRequestDTO componente(
            int productoId, List<PedidoItemOpcionResponseDTO> opciones) {
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
        h.setNombre("Hub EST-" + n);
        h.setEstado("ACTIVO");
        return h;
    }

    private static Club club(Hub hub, Usuario host, int n) {
        Club c = new Club();
        c.setHub(hub);
        c.setAnfitrion(host);
        c.setNombreClub("Club EST-" + n);
        c.setEstado("ACTIVO");
        c.setPrefijoSocio("E" + n);
        return c;
    }

    private static ProductoOpcion opcionEntity(ProductoGrupoOpcion grupo, String nombre, int orden) {
        ProductoOpcion o = new ProductoOpcion();
        o.setGrupo(grupo);
        o.setNombre(nombre);
        o.setOrden(orden);
        o.setActivo(true);
        return o;
    }

    private void authenticateSocio(Integer membresiaId) {
        Membresia membresia = membresiaRepository.findById(membresiaId).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(membresia.getUsuario().getEmail(), "n/a", Collections.emptyList()));
    }

    private void authenticateHost(Integer clubId) {
        Club club = clubRepository.findById(clubId).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(club.getAnfitrion().getEmail(), "n/a", Collections.emptyList()));
    }

    private record Seed(
            Integer membresiaId,
            Integer clubId,
            Integer teId,
            Integer batidoId,
            Integer grupoId,
            Integer opcionId) {
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
            Integer opcionCremosoId) {
    }
}
