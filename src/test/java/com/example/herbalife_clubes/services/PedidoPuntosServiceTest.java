package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.asistencia.AsistenciaDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoComboComponenteRequestDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoComboRequestDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoConItemsDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoMostradorRequestDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Combo;
import com.example.herbalife_clubes.entities.ComboItem;
import com.example.herbalife_clubes.entities.EstadoPedido;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.PointsRecalculationRejectedException;
import com.example.herbalife_clubes.membresias.PointsRecalculationRejections;
import com.example.herbalife_clubes.pedidos.PedidoComboSupport;
import com.example.herbalife_clubes.repositories.AsistenciaRepository;
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.ComboRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.PedidoRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.AsistenciaServiceImpl;
import com.example.herbalife_clubes.serviceimpls.MembresiaServiceImpl;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;

/**
 * POINTS-ORDER-001: puntos por compras entregadas, sin sobrescritura por asistencia.
 */
@Tag("postgres")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.open-in-view=false",
        "attendance.max-distance-meters=5000"
})
@Testcontainers(disabledWithoutDocker = true)
@EnabledIf("dockerAvailable")
@Import({PedidoServiceImpl.class, PedidoComboSupport.class, AsistenciaServiceImpl.class, MembresiaServiceImpl.class})
class PedidoPuntosServiceTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final double CLUB_LAT = -17.3935;
    private static final double CLUB_LNG = -66.1570;

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
    @Autowired private AsistenciaService asistenciaService;
    @Autowired private MembresiaService membresiaService;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private MembresiaRepository membresiaRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ComboRepository comboRepository;
    @Autowired private ClubRepository clubRepository;
    @Autowired private HubRepository hubRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private ClubProductoRepository clubProductoRepository;
    @Autowired private AsistenciaRepository asistenciaRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoBean
    private ComboConsumoService comboConsumoService;
    @MockitoBean
    private MembresiaLogroService membresiaLogroService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // --- PEDIDO SOCIO ---

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void socioPedidoRecibidoNoAcreditaPuntos() {
        Seed seed = seedConProducto(10);
        int puntosAntes = puntosMembresia(seed.membresiaId());

        crearPedidoSuelto(seed, seed.productoId(), 1);

        assertEquals(puntosAntes, puntosMembresia(seed.membresiaId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void socioFlujoEstadosSoloEntregadoAcredita() {
        Seed seed = seedConProducto(10);
        Integer pedidoId = crearPedidoSuelto(seed, seed.productoId(), 1);
        authenticateHost(seed.clubId());

        pedidoService.actualizarEstado(pedidoId, "PREPARANDO", 5);
        assertEquals(0, puntosMembresia(seed.membresiaId()));

        pedidoService.actualizarEstado(pedidoId, "LISTO", null);
        assertEquals(0, puntosMembresia(seed.membresiaId()));

        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);
        assertEquals(10, puntosMembresia(seed.membresiaId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void segundoEntregadoNoDuplicaPuntos() {
        Seed seed = seedConProducto(10);
        Integer pedidoId = crearPedidoSuelto(seed, seed.productoId(), 1);
        authenticateHost(seed.clubId());

        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);
        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);

        assertEquals(10, puntosMembresia(seed.membresiaId()));
        assertTrue(pedidoAcreditado(pedidoId));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void socioPedidoCantidadDosAcreditaVeinte() {
        Seed seed = seedConProducto(10);
        Integer pedidoId = crearPedidoSuelto(seed, seed.productoId(), 2);
        authenticateHost(seed.clubId());

        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);

        assertEquals(20, puntosMembresia(seed.membresiaId()));
        assertEquals(20, puntosGanadosPedido(pedidoId));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void productoSinPuntosAcreditaCeroSinError() {
        Seed seed = seedConProducto(0);
        Integer pedidoId = crearPedidoSuelto(seed, seed.productoId(), 2);
        authenticateHost(seed.clubId());

        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);

        assertEquals(0, puntosMembresia(seed.membresiaId()));
        assertTrue(pedidoAcreditado(pedidoId));
        assertEquals(0, puntosGanadosPedido(pedidoId));
    }

    // --- COMBOS ---

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void comboSnapshotQuincePorDosAcreditaTreinta() {
        SeedCombo seed = seedCombo(15);
        Integer pedidoId = crearPedidoCombo(seed, 2);
        authenticateHost(seed.clubId());

        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);

        assertEquals(30, puntosMembresia(seed.membresiaId()));
        assertEquals(30, puntosGanadosPedido(pedidoId));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void pedidoMixtoComboQuinceMasProductoDiezPorDos() {
        SeedCombo seed = seedCombo(15);
        Integer pedidoId = crearPedidoMixto(seed, 10, 2);
        authenticateHost(seed.clubId());

        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);

        assertEquals(35, puntosMembresia(seed.membresiaId()));
    }

    // --- MOSTRADOR ---

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void mostradorSocioAcreditaUnaSolaVez() {
        Seed seed = seedConProducto(12);
        authenticateHost(seed.clubId());

        PedidoMostradorRequestDTO request = mostradorRequest(seed, seed.productoId(), 1);
        request.setSocioCodigo(seed.numeroSocio());

        PedidoDTO creado = pedidoService.createPedidoMostrador(request);

        assertEquals(12, puntosMembresia(seed.membresiaId()));
        assertTrue(pedidoAcreditado(creado.getId()));
        assertEquals(12, puntosGanadosPedido(creado.getId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void mostradorExternoNoAcredita() {
        Seed seed = seedConProducto(12);
        authenticateHost(seed.clubId());

        PedidoMostradorRequestDTO request = mostradorRequest(seed, seed.productoId(), 1);
        PedidoDTO creado = pedidoService.createPedidoMostrador(request);

        assertEquals(0, puntosMembresia(seed.membresiaId()));
        assertFalse(pedidoAcreditado(creado.getId()));
    }

    // --- ASISTENCIA ---

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void asistenciaNoModificaPuntosAcumulados() {
        Seed seed = seedClubConUbicacion(20);
        doNothing().when(comboConsumoService).validarComboConsumidoAntesDeAsistencia(seed.membresiaId());
        authenticateSocio(seed.membresiaId());

        AsistenciaDTO dto = asistenciaService.registrarAsistencia(
                seed.membresiaId(), seed.clubId(), null, CLUB_LAT, CLUB_LNG, 5.0);

        assertNotNull(dto.getId());
        assertEquals(20, puntosMembresia(seed.membresiaId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void variasAsistenciasMantienenPuntosYRacha() {
        Seed seed = seedClubConUbicacion(15);
        doNothing().when(comboConsumoService).validarComboConsumidoAntesDeAsistencia(seed.membresiaId());
        authenticateSocio(seed.membresiaId());

        asistenciaService.registrarAsistencia(seed.membresiaId(), seed.clubId(), null, CLUB_LAT, CLUB_LNG, 5.0);

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            entityManager.clear();
            java.time.LocalDate ayer = java.time.LocalDate.now().minusDays(1);
            asistenciaRepository.findByMembresiaId(seed.membresiaId()).forEach(a -> {
                a.setFechaDia(ayer);
                asistenciaRepository.save(a);
            });
            Membresia m = membresiaRepository.findById(seed.membresiaId()).orElseThrow();
            m.setUltimaAsistenciaDia(ayer);
            membresiaRepository.save(m);
        });

        asistenciaService.registrarAsistencia(seed.membresiaId(), seed.clubId(), null, CLUB_LAT, CLUB_LNG, 5.0);

        tx.executeWithoutResult(s -> {
            entityManager.clear();
            Membresia m = membresiaRepository.findById(seed.membresiaId()).orElseThrow();
            assertEquals(15, m.getPuntosAcumulados());
            assertEquals(2, m.getRachaActual());
        });
    }

    // --- HISTÓRICO ---

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void historicoSinPuntosGanadosCalculaFallbackAlEntregar() {
        Seed seed = seedConProducto(8);
        Integer pedidoId = crearPedidoHistoricoSinSnapshot(seed, 2);
        authenticateHost(seed.clubId());

        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);

        assertEquals(16, puntosMembresia(seed.membresiaId()));
        assertEquals(16, puntosGanadosPedido(pedidoId));
        assertTrue(pedidoAcreditado(pedidoId));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void historicoSegundoEntregadoNoRecalcula() {
        Seed seed = seedConProducto(8);
        Integer pedidoId = crearPedidoHistoricoSinSnapshot(seed, 1);
        authenticateHost(seed.clubId());

        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);
        cambiarPuntosProducto(seed.productoId(), 99);
        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);

        assertEquals(8, puntosMembresia(seed.membresiaId()));
        assertEquals(8, puntosGanadosPedido(pedidoId));
    }

    // --- CONCURRENCIA ---

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void dobleEntregadoConcurrenteAcreditaUnaVez() throws Exception {
        Seed seed = seedConProducto(10);
        Integer pedidoId = crearPedidoSuelto(seed, seed.productoId(), 1);
        authenticateHost(seed.clubId());
        pedidoService.actualizarEstado(pedidoId, "LISTO", null);

        int threads = 2;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicReference<Throwable> error = new AtomicReference<>();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    TransactionTemplate tx = new TransactionTemplate(transactionManager);
                    tx.executeWithoutResult(s -> {
                        authenticateHost(seed.clubId());
                        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);
                    });
                } catch (Throwable t) {
                    error.compareAndSet(null, t);
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }

        assertEquals(10, puntosMembresia(seed.membresiaId()));
        assertTrue(pedidoAcreditado(pedidoId));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void dosPedidosConcurrentesSumanAmbos() throws Exception {
        Seed seed = seedConProducto(10);
        Integer pedido1 = crearPedidoSuelto(seed, seed.productoId(), 1);
        Integer pedido2 = crearPedidoSuelto(seed, seed.productoId(), 1);
        authenticateHost(seed.clubId());
        pedidoService.actualizarEstado(pedido1, "LISTO", null);
        pedidoService.actualizarEstado(pedido2, "LISTO", null);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicReference<Throwable> error = new AtomicReference<>();

        pool.submit(entregarAsync(pedido1, seed, ready, start, error));
        pool.submit(entregarAsync(pedido2, seed, ready, start, error));

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }

        assertEquals(20, puntosMembresia(seed.membresiaId()));
    }

    // --- CANCELACIÓN ---

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void cancelarAntesDeEntregarNoCambiaPuntos() {
        Seed seed = seedConProducto(10);
        Integer pedidoId = crearPedidoSuelto(seed, seed.productoId(), 1);
        authenticateSocio(seed.membresiaId());

        pedidoService.cancelarPedido(pedidoId);

        assertEquals(0, puntosMembresia(seed.membresiaId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void cancelarEntregadoSigueRechazado() {
        Seed seed = seedConProducto(10);
        Integer pedidoId = crearPedidoSuelto(seed, seed.productoId(), 1);
        authenticateHost(seed.clubId());
        pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);
        authenticateSocio(seed.membresiaId());

        assertThrows(IllegalArgumentException.class, () -> pedidoService.cancelarPedido(pedidoId));
        assertEquals(10, puntosMembresia(seed.membresiaId()));
    }

    // --- RECALCULAR ---

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void recalcularPorAsistenciaRechazaSinSobrescribir() {
        Seed seed = seedConProducto(0);
        fijarPuntos(seed.membresiaId(), 25);

        PointsRecalculationRejectedException ex = assertThrows(PointsRecalculationRejectedException.class,
                () -> membresiaService.recalcularPuntosPorAsistencias(seed.membresiaId()));

        assertEquals(PointsRecalculationRejections.POINTS_RECALCULATION_UNSUPPORTED, ex.getErrorCode());
        assertEquals(25, puntosMembresia(seed.membresiaId()));
    }

    // --- helpers ---

    private Runnable entregarAsync(
            Integer pedidoId, Seed seed, CountDownLatch ready, CountDownLatch start,
            AtomicReference<Throwable> error) {
        return () -> {
            try {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                TransactionTemplate tx = new TransactionTemplate(transactionManager);
                tx.executeWithoutResult(s -> {
                    authenticateHost(seed.clubId());
                    pedidoService.actualizarEstado(pedidoId, "ENTREGADO", null);
                });
            } catch (Throwable t) {
                error.compareAndSet(null, t);
            }
        };
    }

    private Integer crearPedidoSuelto(Seed seed, int productoId, int cantidad) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            authenticateSocio(seed.membresiaId());
            PedidoConItemsDTO request = new PedidoConItemsDTO();
            request.setTipoConsumo("EN_LUGAR");
            PedidoItemDTO item = new PedidoItemDTO();
            item.setProductoId(productoId);
            item.setCantidad(cantidad);
            request.setItems(List.of(item));
            return pedidoService.createPedidoConItems(request, seed.membresiaId(), seed.clubId()).getId();
        });
    }

    private Integer crearPedidoCombo(SeedCombo seed, int cantidadCombo) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            authenticateSocio(seed.membresiaId());
            PedidoConItemsDTO request = new PedidoConItemsDTO();
            request.setTipoConsumo("EN_LUGAR");
            request.setItems(List.of());
            PedidoComboRequestDTO combo = new PedidoComboRequestDTO();
            combo.setComboId(seed.comboId());
            combo.setCantidad(cantidadCombo);
            combo.setComponentes(List.of(
                    componente(seed.teId(), List.of()),
                    componente(seed.batidoId(), List.of())));
            request.setCombos(List.of(combo));
            return pedidoService.createPedidoConItems(request, seed.membresiaId(), seed.clubId()).getId();
        });
    }

    private Integer crearPedidoMixto(SeedCombo seed, int puntosExtraProducto, int cantidadExtra) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            authenticateSocio(seed.membresiaId());
            Producto extra = productoRepository.findById(seed.extraId()).orElseThrow();
            extra.setPuntosValor(puntosExtraProducto);
            productoRepository.save(extra);

            PedidoConItemsDTO request = new PedidoConItemsDTO();
            request.setTipoConsumo("EN_LUGAR");
            PedidoItemDTO suelto = new PedidoItemDTO();
            suelto.setProductoId(seed.extraId());
            suelto.setCantidad(cantidadExtra);
            request.setItems(List.of(suelto));
            PedidoComboRequestDTO combo = new PedidoComboRequestDTO();
            combo.setComboId(seed.comboId());
            combo.setCantidad(1);
            combo.setComponentes(List.of(
                    componente(seed.teId(), List.of()),
                    componente(seed.batidoId(), List.of())));
            request.setCombos(List.of(combo));
            return pedidoService.createPedidoConItems(request, seed.membresiaId(), seed.clubId()).getId();
        });
    }

    private Integer crearPedidoHistoricoSinSnapshot(Seed seed, int cantidad) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            authenticateSocio(seed.membresiaId());
            PedidoConItemsDTO request = new PedidoConItemsDTO();
            request.setTipoConsumo("EN_LUGAR");
            PedidoItemDTO item = new PedidoItemDTO();
            item.setProductoId(seed.productoId());
            item.setCantidad(cantidad);
            request.setItems(List.of(item));
            Integer id = pedidoService.createPedidoConItems(request, seed.membresiaId(), seed.clubId()).getId();
            Pedido pedido = pedidoRepository.findById(id).orElseThrow();
            pedido.setPuntosGanados(null);
            pedido.setPuntosAcreditados(false);
            pedidoRepository.save(pedido);
            return id;
        });
    }

    private void cambiarPuntosProducto(int productoId, int puntos) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            Producto p = productoRepository.findById(productoId).orElseThrow();
            p.setPuntosValor(puntos);
            productoRepository.save(p);
        });
    }

    private void fijarPuntos(int membresiaId, int puntos) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            Membresia m = membresiaRepository.findById(membresiaId).orElseThrow();
            m.setPuntosAcumulados(puntos);
            membresiaRepository.save(m);
        });
    }

    private int puntosMembresia(int membresiaId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(s -> {
            entityManager.clear();
            Membresia m = membresiaRepository.findById(membresiaId).orElseThrow();
            return m.getPuntosAcumulados() != null ? m.getPuntosAcumulados() : 0;
        });
    }

    private int puntosGanadosPedido(int pedidoId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(s -> {
            entityManager.clear();
            Pedido p = pedidoRepository.findById(pedidoId).orElseThrow();
            return p.getPuntosGanados() != null ? p.getPuntosGanados() : 0;
        });
    }

    private boolean pedidoAcreditado(int pedidoId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return Boolean.TRUE.equals(tx.execute(s -> {
            entityManager.clear();
            return pedidoRepository.findById(pedidoId).orElseThrow().getPuntosAcreditados();
        }));
    }

    private static PedidoMostradorRequestDTO mostradorRequest(Seed seed, int productoId, int cantidad) {
        PedidoMostradorRequestDTO request = new PedidoMostradorRequestDTO();
        request.setClubId(seed.clubId());
        request.setTipoPago("EFECTIVO");
        PedidoItemDTO item = new PedidoItemDTO();
        item.setProductoId(productoId);
        item.setCantidad(cantidad);
        request.setItems(List.of(item));
        return request;
    }

    private Seed seedConProducto(int puntosValor) {
        int n = SEQ.incrementAndGet();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rolSocio = rolRepository.save(rol("SOCIO"));
            Usuario socio = usuarioRepository.save(usuario(rolSocio, "pts-socio-" + n + "@test.com"));
            Rol rolHost = rolRepository.save(rol("ANFITRION"));
            Usuario host = usuarioRepository.save(usuario(rolHost, "pts-host-" + n + "@test.com"));
            Hub hub = hubRepository.save(hub(host, n));
            Club club = clubRepository.save(club(hub, host, n));

            Membresia membresia = new Membresia();
            membresia.setUsuario(socio);
            membresia.setClub(club);
            membresia.setNumeroSocio("PTS-" + n);
            membresia.setEstado("ACTIVA");
            membresia.setPuntosAcumulados(0);
            membresia = membresiaRepository.save(membresia);

            Producto producto = saveProducto(club, hub, "Prod-" + n, bd("20.00"), puntosValor);
            return new Seed(membresia.getId(), club.getId(), producto.getId(), membresia.getNumeroSocio());
        });
    }

    private Seed seedClubConUbicacion(int puntosIniciales) {
        int n = SEQ.incrementAndGet();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rolSocio = rolRepository.save(rol("SOCIO"));
            Usuario socio = usuarioRepository.save(usuario(rolSocio, "pts-asist-" + n + "@test.com"));
            Rol rolHost = rolRepository.save(rol("ANFITRION"));
            Usuario host = usuarioRepository.save(usuario(rolHost, "pts-asist-host-" + n + "@test.com"));
            Hub hub = hubRepository.save(hub(host, n));
            Club club = club(hub, host, n);
            club.setLat(BigDecimal.valueOf(CLUB_LAT));
            club.setLng(BigDecimal.valueOf(CLUB_LNG));
            club = clubRepository.save(club);

            Membresia membresia = new Membresia();
            membresia.setUsuario(socio);
            membresia.setClub(club);
            membresia.setNumeroSocio("PTA-" + n);
            membresia.setEstado("ACTIVA");
            membresia.setPuntosAcumulados(puntosIniciales);
            membresia = membresiaRepository.save(membresia);

            saveProducto(club, hub, "Prod-" + n, bd("15.00"), 0);
            return new Seed(membresia.getId(), club.getId(), 0, membresia.getNumeroSocio());
        });
    }

    private SeedCombo seedCombo(int puntosCombo) {
        int n = SEQ.incrementAndGet();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rolSocio = rolRepository.save(rol("SOCIO"));
            Usuario socio = usuarioRepository.save(usuario(rolSocio, "pts-combo-" + n + "@test.com"));
            Rol rolHost = rolRepository.save(rol("ANFITRION"));
            Usuario host = usuarioRepository.save(usuario(rolHost, "pts-combo-host-" + n + "@test.com"));
            Hub hub = hubRepository.save(hub(host, n));
            Club club = clubRepository.save(club(hub, host, n));

            Membresia membresia = new Membresia();
            membresia.setUsuario(socio);
            membresia.setClub(club);
            membresia.setNumeroSocio("PTC-" + n);
            membresia.setEstado("ACTIVA");
            membresia.setPuntosAcumulados(0);
            membresia = membresiaRepository.save(membresia);

            Producto te = saveProducto(club, hub, "Té", bd("10.00"), 3);
            Producto batido = saveProducto(club, hub, "Batido", bd("20.00"), 5);
            Producto extra = saveProducto(club, hub, "Extra", bd("12.00"), 10);

            Combo combo = new Combo();
            combo.setClub(club);
            combo.setNombre("Combo pts");
            combo.setPrecio(bd("30.00"));
            combo.setActivo(true);
            combo.setPuntosValor(puntosCombo);
            combo.setItems(new ArrayList<>());
            combo.getItems().add(comboItem(combo, te, 1));
            combo.getItems().add(comboItem(combo, batido, 1));
            combo = comboRepository.saveAndFlush(combo);

            return new SeedCombo(
                    membresia.getId(), club.getId(), combo.getId(),
                    te.getId(), batido.getId(), extra.getId());
        });
    }

    private Producto saveProducto(Club club, Hub hub, String nombre, BigDecimal precio, int puntos) {
        Producto producto = new Producto();
        producto.setHub(hub);
        producto.setClubCreador(club);
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setPuntosValor(puntos);
        producto.setTipo("LOCAL");
        producto.setEstadoAprobacion("APROBADO");
        producto.setActivo(true);
        producto.setGruposOpciones(new ArrayList<>());
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

    private static PedidoComboComponenteRequestDTO componente(int productoId, List<?> opciones) {
        PedidoComboComponenteRequestDTO dto = new PedidoComboComponenteRequestDTO();
        dto.setProductoId(productoId);
        dto.setOpciones(Collections.emptyList());
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
        h.setNombre("Hub PTS-" + n);
        h.setEstado("ACTIVO");
        return h;
    }

    private static Club club(Hub hub, Usuario host, int n) {
        Club c = new Club();
        c.setHub(hub);
        c.setAnfitrion(host);
        c.setNombreClub("Club PTS-" + n);
        c.setEstado("ACTIVO");
        c.setPrefijoSocio("P" + n);
        return c;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private void authenticateSocio(int membresiaId) {
        Membresia m = membresiaRepository.findById(membresiaId).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(m.getUsuario().getEmail(), "n/a", Collections.emptyList()));
    }

    private void authenticateHost(int clubId) {
        Club club = clubRepository.findById(clubId).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(club.getAnfitrion().getEmail(), "n/a", Collections.emptyList()));
    }

    private record Seed(Integer membresiaId, Integer clubId, Integer productoId, String numeroSocio) {
    }

    private record SeedCombo(
            Integer membresiaId, Integer clubId, Integer comboId,
            Integer teId, Integer batidoId, Integer extraId) {
    }
}
