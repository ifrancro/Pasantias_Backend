package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.asistencias.AttendanceLocationRejections;
import com.example.herbalife_clubes.dtos.asistencia.AsistenciaDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.AttendanceLocationRejectedException;
import com.example.herbalife_clubes.exceptions.ComboRequiredException;
import com.example.herbalife_clubes.repositories.AsistenciaRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.AsistenciaServiceImpl;
import com.example.herbalife_clubes.util.GeoDistance;
import org.junit.jupiter.api.AfterEach;
import org.springframework.test.util.ReflectionTestUtils;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

/**
 * MOB-ATT-002: validación autoritativa de ubicación en registro de asistencia.
 */
@Tag("postgres")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.open-in-view=false",
        "attendance.max-distance-meters=100"
})
@Testcontainers(disabledWithoutDocker = true)
@EnabledIf("dockerAvailable")
@Import(AsistenciaServiceImpl.class)
class AsistenciaLocationPersistenceTest {

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

    @Autowired private AsistenciaService asistenciaService;
    @Autowired private AsistenciaServiceImpl asistenciaServiceImpl;
    @Autowired private AsistenciaRepository asistenciaRepository;
    @Autowired private MembresiaRepository membresiaRepository;
    @Autowired private ClubRepository clubRepository;
    @Autowired private HubRepository hubRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoBean
    private ComboConsumoService comboConsumoService;
    @MockitoBean
    private MembresiaLogroService membresiaLogroService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void dentroDelRadioRegistra() {
        Seed seed = seedActivoConUbicacion();
        mockComboOk(seed.membresiaId());
        long antes = asistenciaRepository.findByMembresiaId(seed.membresiaId()).size();

        AsistenciaDTO creada = registrar(seed, CLUB_LAT, CLUB_LNG);

        assertNotNull(creada.getId());
        assertEquals("CONFIRMADA", creada.getEstado());
        assertEquals(antes + 1, asistenciaRepository.findByMembresiaId(seed.membresiaId()).size());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void exactamenteEnBordeRegistra() {
        Seed seed = seedActivoConUbicacion();
        mockComboOk(seed.membresiaId());
        double borderLat = puntoExactamenteEnBorde(100.0);

        AsistenciaDTO creada = registrar(seed, borderLat, CLUB_LNG);

        assertNotNull(creada.getId());
        double dist = GeoDistance.distanceMeters(borderLat, CLUB_LNG, CLUB_LAT, CLUB_LNG);
        assertTrue(dist <= 100.0, "distancia borde=" + dist);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void fueraDelRadioOutOfRange() {
        Seed seed = seedActivoConUbicacion();
        mockComboOk(seed.membresiaId());
        double lejosLat = CLUB_LAT + (150.0 / 111_320.0);

        AttendanceLocationRejectedException ex = assertThrows(AttendanceLocationRejectedException.class,
                () -> registrar(seed, lejosLat, CLUB_LNG));

        assertEquals(AttendanceLocationRejections.ATTENDANCE_OUT_OF_RANGE, ex.getErrorCode());
        assertEquals(100.0, ex.getMaxDistanceMeters());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void fueraDelRadioNoCreaAsistencia() {
        Seed seed = seedActivoConUbicacion();
        mockComboOk(seed.membresiaId());
        long antes = asistenciaRepository.findByMembresiaId(seed.membresiaId()).size();
        double lejosLat = CLUB_LAT + (150.0 / 111_320.0);

        assertThrows(AttendanceLocationRejectedException.class,
                () -> registrar(seed, lejosLat, CLUB_LNG));

        assertEquals(antes, asistenciaRepository.findByMembresiaId(seed.membresiaId()).size());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void fueraDelRadioNoCambiaRacha() {
        Seed seed = seedActivoConUbicacion();
        mockComboOk(seed.membresiaId());
        Membresia antes = membresiaRepository.findById(seed.membresiaId()).orElseThrow();
        int rachaAntes = antes.getRachaActual();
        int maxAntes = antes.getRachaMaxima();
        LocalDate ultimaAntes = antes.getUltimaAsistenciaDia();
        double lejosLat = CLUB_LAT + (150.0 / 111_320.0);

        assertThrows(AttendanceLocationRejectedException.class,
                () -> registrar(seed, lejosLat, CLUB_LNG));

        Membresia despues = membresiaRepository.findById(seed.membresiaId()).orElseThrow();
        assertEquals(rachaAntes, despues.getRachaActual());
        assertEquals(maxAntes, despues.getRachaMaxima());
        assertEquals(ultimaAntes, despues.getUltimaAsistenciaDia());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void fueraDelRadioNoCambiaPuntos() {
        Seed seed = seedActivoConUbicacion();
        mockComboOk(seed.membresiaId());
        Integer puntosAntes = membresiaRepository.findById(seed.membresiaId()).orElseThrow().getPuntosAcumulados();
        double lejosLat = CLUB_LAT + (150.0 / 111_320.0);

        assertThrows(AttendanceLocationRejectedException.class,
                () -> registrar(seed, lejosLat, CLUB_LNG));

        Integer puntosDespues = membresiaRepository.findById(seed.membresiaId()).orElseThrow().getPuntosAcumulados();
        assertEquals(puntosAntes, puntosDespues);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void latitudMissingLocationRequired() {
        Seed seed = seedActivoConUbicacion();
        authenticate(seed.socioEmail());

        AttendanceLocationRejectedException ex = assertThrows(AttendanceLocationRejectedException.class,
                () -> txRegistrar(seed, null, CLUB_LNG));

        assertEquals(AttendanceLocationRejections.ATTENDANCE_LOCATION_REQUIRED, ex.getErrorCode());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void longitudMissingLocationRequired() {
        Seed seed = seedActivoConUbicacion();
        authenticate(seed.socioEmail());

        AttendanceLocationRejectedException ex = assertThrows(AttendanceLocationRejectedException.class,
                () -> txRegistrar(seed, CLUB_LAT, null));

        assertEquals(AttendanceLocationRejections.ATTENDANCE_LOCATION_REQUIRED, ex.getErrorCode());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void latitudMayor90LocationInvalid() {
        Seed seed = seedActivoConUbicacion();
        authenticate(seed.socioEmail());

        AttendanceLocationRejectedException ex = assertThrows(AttendanceLocationRejectedException.class,
                () -> txRegistrar(seed, 91.0, CLUB_LNG));

        assertEquals(AttendanceLocationRejections.ATTENDANCE_LOCATION_INVALID, ex.getErrorCode());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void longitudMayor180LocationInvalid() {
        Seed seed = seedActivoConUbicacion();
        authenticate(seed.socioEmail());

        AttendanceLocationRejectedException ex = assertThrows(AttendanceLocationRejectedException.class,
                () -> txRegistrar(seed, CLUB_LAT, 181.0));

        assertEquals(AttendanceLocationRejections.ATTENDANCE_LOCATION_INVALID, ex.getErrorCode());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void nanLocationInvalid() {
        Seed seed = seedActivoConUbicacion();
        authenticate(seed.socioEmail());

        AttendanceLocationRejectedException ex = assertThrows(AttendanceLocationRejectedException.class,
                () -> txRegistrar(seed, Double.NaN, CLUB_LNG));

        assertEquals(AttendanceLocationRejections.ATTENDANCE_LOCATION_INVALID, ex.getErrorCode());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void clubLatNullClubLocationUnavailable() {
        Seed seed = seedActivoSinUbicacion();
        authenticate(seed.socioEmail());
        mockComboOk(seed.membresiaId());

        AttendanceLocationRejectedException ex = assertThrows(AttendanceLocationRejectedException.class,
                () -> txRegistrar(seed, CLUB_LAT, CLUB_LNG));

        assertEquals(AttendanceLocationRejections.ATTENDANCE_CLUB_LOCATION_UNAVAILABLE, ex.getErrorCode());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void clubLngNullClubLocationUnavailable() {
        Seed seed = seedActivoSinUbicacion();
        Club club = clubRepository.findById(seed.clubId()).orElseThrow();
        club.setLat(BigDecimal.valueOf(CLUB_LAT));
        clubRepository.save(club);
        authenticate(seed.socioEmail());
        mockComboOk(seed.membresiaId());

        AttendanceLocationRejectedException ex = assertThrows(AttendanceLocationRejectedException.class,
                () -> txRegistrar(seed, CLUB_LAT, CLUB_LNG));

        assertEquals(AttendanceLocationRejections.ATTENDANCE_CLUB_LOCATION_UNAVAILABLE, ex.getErrorCode());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void membershipAjena403() {
        TwoSocioSeed seed = seedDosSocios();
        authenticate(seed.socioBEmail());
        mockComboOk(seed.membresiaAId());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> txRegistrarAs(seed.membresiaAId(), seed.clubId(), seed.socioBEmail(), CLUB_LAT, CLUB_LNG));

        assertEquals("No tienes permisos para usar esta membresía.", ex.getMessage());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void ownMembershipContinua() {
        Seed seed = seedActivoConUbicacion();
        mockComboOk(seed.membresiaId());

        assertDoesNotThrow(() -> registrar(seed, CLUB_LAT, CLUB_LNG));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void membershipInactivaSigueRechazando() {
        Seed seed = seedActivoConUbicacion();
        Membresia m = membresiaRepository.findById(seed.membresiaId()).orElseThrow();
        m.setEstado("SUSPENDIDA");
        membresiaRepository.save(m);
        authenticate(seed.socioEmail());
        mockComboOk(seed.membresiaId());

        assertThrows(IllegalArgumentException.class,
                () -> txRegistrar(seed, CLUB_LAT, CLUB_LNG));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void clubInactivoSigueRechazando() {
        Seed seed = seedActivoConUbicacion();
        Club club = clubRepository.findById(seed.clubId()).orElseThrow();
        club.setEstado("PENDIENTE");
        clubRepository.save(club);
        authenticate(seed.socioEmail());
        mockComboOk(seed.membresiaId());

        assertThrows(IllegalArgumentException.class,
                () -> txRegistrar(seed, CLUB_LAT, CLUB_LNG));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void comboRequiredSigueFuncionando() {
        Seed seed = seedActivoConUbicacion();
        authenticate(seed.socioEmail());
        doThrow(new ComboRequiredException("Debes consumir un combo antes de registrar asistencia"))
                .when(comboConsumoService).validarComboConsumidoAntesDeAsistencia(seed.membresiaId());

        assertThrows(ComboRequiredException.class,
                () -> txRegistrar(seed, CLUB_LAT, CLUB_LNG));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void asistenciaDuplicadaDiariaSigueFuncionando() {
        Seed seed = seedActivoConUbicacion();
        mockComboOk(seed.membresiaId());
        registrar(seed, CLUB_LAT, CLUB_LNG);

        assertThrows(IllegalArgumentException.class,
                () -> registrar(seed, CLUB_LAT, CLUB_LNG));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void radioConfigurableSeRespeta() {
        ReflectionTestUtils.setField(asistenciaServiceImpl, "maxDistanceMeters", 50.0);
        Seed seed = seedActivoConUbicacion();
        mockComboOk(seed.membresiaId());
        double lat75m = CLUB_LAT + (75.0 / 111_320.0);
        double dist = GeoDistance.distanceMeters(lat75m, CLUB_LNG, CLUB_LAT, CLUB_LNG);
        assertTrue(dist > 50.0 && dist < 100.0, "distancia test=" + dist);

        AttendanceLocationRejectedException ex = assertThrows(AttendanceLocationRejectedException.class,
                () -> registrar(seed, lat75m, CLUB_LNG));

        assertEquals(AttendanceLocationRejections.ATTENDANCE_OUT_OF_RANGE, ex.getErrorCode());
        assertEquals(50.0, ex.getMaxDistanceMeters());
    }

    private double puntoExactamenteEnBorde(double maxMetros) {
        double low = 0;
        double high = maxMetros / 50_000.0;
        double candidateLat = CLUB_LAT;
        for (int i = 0; i < 40; i++) {
            double mid = (low + high) / 2;
            candidateLat = CLUB_LAT + mid;
            double dist = GeoDistance.distanceMeters(candidateLat, CLUB_LNG, CLUB_LAT, CLUB_LNG);
            if (dist > maxMetros) {
                high = mid;
            } else {
                low = mid;
            }
        }
        return candidateLat;
    }

    private void mockComboOk(Integer membresiaId) {
        doNothing().when(comboConsumoService).validarComboConsumidoAntesDeAsistencia(membresiaId);
        doNothing().when(membresiaLogroService).evaluarLogrosAutomaticamente(membresiaId);
    }

    private AsistenciaDTO registrar(Seed seed, double lat, double lng) {
        authenticate(seed.socioEmail());
        return txRegistrar(seed, lat, lng);
    }

    private AsistenciaDTO txRegistrar(Seed seed, Double lat, Double lng) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> asistenciaService.registrarAsistencia(
                seed.membresiaId(), seed.clubId(), null, lat, lng, null));
    }

    private AsistenciaDTO txRegistrarAs(
            Integer membresiaId, Integer clubId, String email, double lat, double lng) {
        authenticate(email);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> asistenciaService.registrarAsistencia(
                membresiaId, clubId, null, lat, lng, null));
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "n/a", Collections.emptyList()));
    }

    private Seed seedActivoConUbicacion() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> persistMembresiaYClub(true));
    }

    private Seed seedActivoSinUbicacion() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> persistMembresiaYClub(false));
    }

    private TwoSocioSeed seedDosSocios() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            int n = SEQ.incrementAndGet();
            Rol rolSocio = rolRepository.save(rol("SOCIO"));
            Usuario socioA = usuarioRepository.save(usuario(rolSocio, "loc-a-" + n + "@test.com"));
            Usuario socioB = usuarioRepository.save(usuario(rolSocio, "loc-b-" + n + "@test.com"));
            Rol rolHost = rolRepository.save(rol("ANFITRION"));
            Usuario host = usuarioRepository.save(usuario(rolHost, "loc-host-" + n + "@test.com"));
            Hub hub = hubRepository.save(hub(host, n));
            Club club = clubRepository.save(clubConUbicacion(hub, host, n));

            Membresia membresiaA = membresia(socioA, club, "LOC-A-" + n);
            membresiaA = membresiaRepository.save(membresiaA);
            Membresia membresiaB = membresia(socioB, club, "LOC-B-" + n);
            membresiaB = membresiaRepository.save(membresiaB);

            return new TwoSocioSeed(
                    membresiaA.getId(), membresiaB.getId(), club.getId(), socioB.getEmail());
        });
    }

    private Seed persistMembresiaYClub(boolean conUbicacion) {
        int n = SEQ.incrementAndGet();
        Rol rol = rolRepository.save(rol("SOCIO"));
        Usuario socio = usuarioRepository.save(usuario(rol, "loc-socio-" + n + "@test.com"));
        Rol rolHost = rolRepository.save(rol("ANFITRION"));
        Usuario host = usuarioRepository.save(usuario(rolHost, "loc-host2-" + n + "@test.com"));
        Hub hub = hubRepository.save(hub(host, n));
        Club club = conUbicacion
                ? clubRepository.save(clubConUbicacion(hub, host, n))
                : clubRepository.save(clubSinUbicacion(hub, host, n));

        Membresia membresia = membresia(socio, club, "LOC-" + n);
        membresia = membresiaRepository.save(membresia);
        return new Seed(membresia.getId(), club.getId(), socio.getEmail());
    }

    private static Membresia membresia(Usuario socio, Club club, String numero) {
        Membresia m = new Membresia();
        m.setUsuario(socio);
        m.setClub(club);
        m.setNumeroSocio(numero);
        m.setEstado("ACTIVA");
        m.setRachaActual(0);
        m.setRachaMaxima(0);
        m.setPuntosAcumulados(0);
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
        h.setNombre("Hub LOC-" + n);
        h.setEstado("ACTIVO");
        return h;
    }

    private static Club clubConUbicacion(Hub hub, Usuario host, int n) {
        Club c = clubBase(hub, host, n);
        c.setLat(BigDecimal.valueOf(CLUB_LAT));
        c.setLng(BigDecimal.valueOf(CLUB_LNG));
        return c;
    }

    private static Club clubSinUbicacion(Hub hub, Usuario host, int n) {
        return clubBase(hub, host, n);
    }

    private static Club clubBase(Hub hub, Usuario host, int n) {
        Club c = new Club();
        c.setHub(hub);
        c.setAnfitrion(host);
        c.setNombreClub("Club LOC-" + n);
        c.setEstado("ACTIVO");
        c.setPrefijoSocio("LC" + n);
        return c;
    }

    private record Seed(Integer membresiaId, Integer clubId, String socioEmail) {
    }

    private record TwoSocioSeed(Integer membresiaAId, Integer membresiaBId, Integer clubId, String socioBEmail) {
    }
}
