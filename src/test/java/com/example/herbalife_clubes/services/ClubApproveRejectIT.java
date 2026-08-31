package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.club.ClubDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.NotificacionRepository;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.ClubServiceImpl;
import com.example.herbalife_clubes.serviceimpls.NotificacionServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CLUB-002: escrituras de club con EntityGraph de findById, sin mapear el retorno de save(),
 * y con transacción de escritura. NotificacionServiceImpl real.
 */
@Tag("postgres")
@EnabledIf("localDiagDbAvailable")
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/club_diag_it",
        "spring.datasource.username=${user.name}",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.open-in-view=false",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ClubServiceImpl.class, NotificacionServiceImpl.class})
class ClubApproveRejectIT {

    static boolean localDiagDbAvailable() {
        try (var ignored = java.sql.DriverManager.getConnection(
                "jdbc:postgresql://127.0.0.1:5432/club_diag_it",
                System.getProperty("user.name"),
                "")) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Autowired
    private ClubService clubService;
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private HubRepository hubRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private NotificacionRepository notificacionRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void aprobarClubDevuelveDtoConRelacionesYPersisteRolYNotificacion() {
        SeededClub seeded = seedPendingClub();

        ClubDTO dto = assertDoesNotThrow(() -> clubService.aprobarClub(seeded.clubId()));

        assertClubRelaciones(dto, seeded);
        assertEquals("ACTIVO", dto.getEstado());
        assertEquals("ACTIVO", clubRepository.findById(seeded.clubId()).orElseThrow().getEstado());
        assertEquals("ANFITRION",
                usuarioRepository.findById(seeded.anfitrionId()).orElseThrow().getRol().getNombre());
        assertFalse(notificacionRepository.findByClubId(seeded.clubId()).isEmpty());
        assertEquals("Solicitud de Club Aprobada",
                notificacionRepository.findByClubId(seeded.clubId()).get(0).getTitulo());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void rechazarClubDevuelveDtoConRelacionesYCreaNotificacion() {
        SeededClub seeded = seedPendingClub();

        ClubDTO dto = assertDoesNotThrow(() -> clubService.rechazarClub(seeded.clubId()));

        assertClubRelaciones(dto, seeded);
        assertEquals("RECHAZADO", dto.getEstado());
        assertEquals("RECHAZADO", clubRepository.findById(seeded.clubId()).orElseThrow().getEstado());
        assertFalse(notificacionRepository.findByClubId(seeded.clubId()).isEmpty());
        assertEquals("Solicitud de Club Rechazada",
                notificacionRepository.findByClubId(seeded.clubId()).get(0).getTitulo());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void activarClubDevuelveActivoSinNotificacion() {
        SeededClub seeded = seedPendingClub();

        ClubDTO dto = assertDoesNotThrow(() -> clubService.activarClub(seeded.clubId()));

        assertClubRelaciones(dto, seeded);
        assertEquals("ACTIVO", dto.getEstado());
        assertTrue(notificacionRepository.findByClubId(seeded.clubId()).isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void desactivarClubDevuelveInactivoSinNotificacion() {
        SeededClub seeded = seedPendingClub();

        ClubDTO dto = assertDoesNotThrow(() -> clubService.desactivarClub(seeded.clubId()));

        assertClubRelaciones(dto, seeded);
        assertEquals("INACTIVO", dto.getEstado());
        assertTrue(notificacionRepository.findByClubId(seeded.clubId()).isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void updateClubDevuelveRelacionesTrasSave() {
        SeededClub seeded = seedPendingClub();
        ClubDTO request = new ClubDTO();
        request.setNombreClub("Club actualizado");
        request.setDireccion("Nueva direccion");
        request.setHorario("Lun-Vie");
        request.setPrefijoSocio("PD");
        request.setLat(new BigDecimal("-17.3935"));
        request.setLng(new BigDecimal("-66.1570"));

        ClubDTO dto = assertDoesNotThrow(() -> clubService.updateClub(seeded.clubId(), request));

        assertClubRelaciones(dto, seeded);
        assertEquals("Club actualizado", dto.getNombreClub());
        assertEquals("PENDIENTE", dto.getEstado());
    }

    private void assertClubRelaciones(ClubDTO dto, SeededClub seeded) {
        assertNotNull(dto);
        assertEquals(seeded.clubId(), dto.getId());
        assertEquals(seeded.hubId(), dto.getHubId());
        assertEquals("HUB Santa Cruz", dto.getHubNombre());
        assertEquals(seeded.anfitrionId(), dto.getAnfitrionId());
        assertEquals("Andrea Anfitriona", dto.getAnfitrionNombre());
    }

    private SeededClub seedPendingClub() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol anfitrionRol = rolRepository.findByNombre("ANFITRION").orElseGet(() -> {
                Rol rol = new Rol();
                rol.setNombre("ANFITRION");
                return rolRepository.save(rol);
            });
            Rol basico = new Rol();
            basico.setNombre("USUARIO_BASICO-" + System.nanoTime());
            basico = rolRepository.save(basico);

            Usuario admin = usuario("Admin", "Corporativo", "admin-" + System.nanoTime() + "@it.com", anfitrionRol);
            Usuario host = usuario("Andrea", "Anfitriona", "host-" + System.nanoTime() + "@it.com", basico);

            Hub hub = new Hub();
            hub.setAdmin(admin);
            hub.setNombre("HUB Santa Cruz");
            hub.setEstado("ACTIVO");
            hub = hubRepository.save(hub);

            Club club = new Club();
            club.setHub(hub);
            club.setAnfitrion(host);
            club.setNombreClub("Club pendiente");
            club.setEstado("PENDIENTE");
            club.setPrefijoSocio("PD");
            club.setLat(new BigDecimal("-17.3935"));
            club.setLng(new BigDecimal("-66.1570"));
            club = clubRepository.save(club);
            return new SeededClub(club.getId(), host.getId(), hub.getId());
        });
    }

    private Usuario usuario(String nombre, String apellido, String email, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setEmail(email);
        usuario.setPasswordHash("x");
        usuario.setEstado("ACTIVO");
        return usuarioRepository.save(usuario);
    }

    private record SeededClub(Integer clubId, Integer anfitrionId, Integer hubId) {
    }
}
