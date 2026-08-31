package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.asistencia.AsistenciaDTO;
import com.example.herbalife_clubes.entities.Asistencia;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.AsistenciaRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.AsistenciaServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
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

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;

/**
 * ATT-LIST-001: listados de asistencia mapean membresía/club LAZY dentro de la transacción del servicio.
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
@Import(AsistenciaServiceImpl.class)
class AsistenciaListadoPersistenceTest {

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

    @Autowired private AsistenciaService asistenciaService;
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

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void listarAsistenciasBySocioDevuelveDtoCompletoConRelacionesLazy() {
        Seed seed = seedConAsistencia();

        List<AsistenciaDTO> listado = asistenciaService.listarAsistenciasBySocio(seed.membresiaId());

        assertEquals(1, listado.size());
        AsistenciaDTO dto = listado.get(0);
        assertEquals(seed.membresiaId(), dto.getMembresiaId());
        assertEquals("SOC-" + seed.n(), dto.getMembresiaNumeroSocio());
        assertEquals(seed.clubId(), dto.getClubId());
        assertEquals("Club AS-" + seed.n(), dto.getClubNombre());
        assertEquals("CONFIRMADA", dto.getEstado());
        assertEquals(1, dto.getRachaActual());
        assertEquals(1, dto.getRachaMaxima());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void listarAsistenciasByClubFunciona() {
        Seed seed = seedConAsistencia();

        List<AsistenciaDTO> listado = asistenciaService.listarAsistenciasByClub(seed.clubId());

        assertEquals(1, listado.size());
        assertEquals(seed.clubId(), listado.get(0).getClubId());
        assertEquals("Club AS-" + seed.n(), listado.get(0).getClubNombre());
        assertNotNull(listado.get(0).getMembresiaId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void listarTodasAsistenciasFunciona() {
        Seed seed = seedConAsistencia();

        List<AsistenciaDTO> listado = asistenciaService.listarTodasAsistencias();

        assertFalse(listado.isEmpty());
        assertTrue(listado.stream().anyMatch(d -> seed.membresiaId().equals(d.getMembresiaId())));
        assertTrue(listado.stream().anyMatch(d -> seed.clubId().equals(d.getClubId())));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void registrarAsistenciaSigueFuncionando() {
        Seed seed = seedSinAsistencia();
        doNothing().when(comboConsumoService).validarComboConsumidoAntesDeAsistencia(seed.membresiaId());
        doNothing().when(membresiaLogroService).evaluarLogrosAutomaticamente(seed.membresiaId());

        AsistenciaDTO creada = asistenciaService.registrarAsistencia(seed.membresiaId(), seed.clubId(), null);

        assertNotNull(creada.getId());
        assertEquals("CONFIRMADA", creada.getEstado());
        assertEquals(1, creada.getRachaActual());
        assertEquals(1, creada.getRachaMaxima());

        List<AsistenciaDTO> listado = asistenciaService.listarAsistenciasBySocio(seed.membresiaId());
        assertEquals(1, listado.size());
        assertEquals(creada.getId(), listado.get(0).getId());
    }

    private Seed seedConAsistencia() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Seed seed = persistMembresiaYClub();
            Asistencia asistencia = new Asistencia();
            asistencia.setMembresia(membresiaRepository.findById(seed.membresiaId()).orElseThrow());
            asistencia.setClub(clubRepository.findById(seed.clubId()).orElseThrow());
            asistencia.setFechaDia(LocalDate.now());
            asistencia.setEstado("CONFIRMADA");
            asistenciaRepository.save(asistencia);
            return seed;
        });
    }

    private Seed seedSinAsistencia() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> persistMembresiaYClub());
    }

    private Seed persistMembresiaYClub() {
        int n = SEQ.incrementAndGet();
        Rol rol = rolRepository.save(rol("SOCIO"));
        Usuario socio = usuarioRepository.save(usuario(rol, "asist-socio-" + n + "@test.com"));
        Rol rolHost = rolRepository.save(rol("ANFITRION"));
        Usuario host = usuarioRepository.save(usuario(rolHost, "asist-host-" + n + "@test.com"));
        Hub hub = hubRepository.save(hub(host, n));
        Club club = clubRepository.save(club(hub, host, n));

        Membresia membresia = new Membresia();
        membresia.setUsuario(socio);
        membresia.setClub(club);
        membresia.setNumeroSocio("SOC-" + n);
        membresia.setEstado("ACTIVA");
        membresia.setRachaActual(1);
        membresia.setRachaMaxima(1);
        membresia = membresiaRepository.save(membresia);
        return new Seed(n, membresia.getId(), club.getId());
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
        h.setNombre("Hub AS-" + n);
        h.setEstado("ACTIVO");
        return h;
    }

    private static Club club(Hub hub, Usuario host, int n) {
        Club c = new Club();
        c.setHub(hub);
        c.setAnfitrion(host);
        c.setNombreClub("Club AS-" + n);
        c.setEstado("ACTIVO");
        c.setPrefijoSocio("AS" + n);
        return c;
    }

    private record Seed(int n, Integer membresiaId, Integer clubId) {
    }
}
