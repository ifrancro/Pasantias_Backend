package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.dtos.club.ClubDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.mappers.ClubMapper;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba real de CLUB-001: con open-in-view=false, findAll()/findByAnfitrionId
 * deben devolver hub y anfitrion ya inicializados para ClubMapper.
 */
@Tag("postgres")
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.open-in-view=false",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@EnabledIf("dockerAvailable")
class ClubRepositoryFetchIT {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("club_fetch_it")
                    .withUsername("test")
                    .withPassword("test");

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private HubRepository hubRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void findAllCargaHubYAnfitrionYMapperNoLanzaLazy() {
        Integer clubId = seedClub();

        List<Club> clubes = clubRepository.findAll();
        assertFalse(clubes.isEmpty());
        Club club = clubes.stream().filter(c -> clubId.equals(c.getId())).findFirst().orElseThrow();

        assertTrue(Hibernate.isInitialized(club.getHub()));
        assertTrue(Hibernate.isInitialized(club.getAnfitrion()));

        ClubDTO dto = assertDoesNotThrow(() -> ClubMapper.mapClubToClubDTO(club));
        assertEquals("HUB Santa Cruz", dto.getHubNombre());
        assertEquals("Andrea Anfitriona", dto.getAnfitrionNombre());
        assertEquals(club.getHub().getId(), dto.getHubId());
        assertEquals(club.getAnfitrion().getId(), dto.getAnfitrionId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void findByAnfitrionIdYConsultasPublicasCarganRelaciones() {
        Integer clubId = seedClub();
        Club seeded = clubRepository.findById(clubId).orElseThrow();
        Integer anfitrionId = seeded.getAnfitrion().getId();
        Integer hubId = seeded.getHub().getId();

        Club mio = clubRepository.findByAnfitrionId(anfitrionId).get(0);
        ClubDTO mioDto = ClubMapper.mapClubToClubDTO(mio);
        assertEquals("HUB Santa Cruz", mioDto.getHubNombre());
        assertEquals("Andrea Anfitriona", mioDto.getAnfitrionNombre());

        Club publico = clubRepository.findByIdAndEstadoIn(clubId, List.of("ACTIVO", "APROBADO")).orElseThrow();
        ClubDTO publicoDto = ClubMapper.mapClubToClubDTO(publico);
        assertEquals("HUB Santa Cruz", publicoDto.getHubNombre());

        List<Club> porHub = clubRepository.findByHubId(hubId);
        assertFalse(porHub.isEmpty());
        assertDoesNotThrow(() -> ClubMapper.mapClubToClubDTO(porHub.get(0)));

        List<Club> activos = clubRepository.findByEstadoIn(List.of("ACTIVO", "APROBADO"));
        assertFalse(activos.isEmpty());
        assertDoesNotThrow(() -> ClubMapper.mapClubToClubDTO(activos.get(0)));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void findByIdInexistenteSigueVacio() {
        Optional<Club> missing = clubRepository.findById(999_999);
        assertTrue(missing.isEmpty());
        assertTrue(clubRepository.findByIdAndEstadoIn(999_999, List.of("ACTIVO", "APROBADO")).isEmpty());
        assertTrue(clubRepository.findByAnfitrionId(999_999).isEmpty());
    }

    private Integer seedClub() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rol = new Rol();
            rol.setNombre("ANFITRION-" + System.nanoTime());
            rol = rolRepository.save(rol);

            Usuario admin = usuario("Admin", "Corporativo", "admin-" + System.nanoTime() + "@it.com", rol);
            Usuario anfitrion = usuario("Andrea", "Anfitriona", "host-" + System.nanoTime() + "@it.com", rol);

            Hub hub = new Hub();
            hub.setAdmin(admin);
            hub.setNombre("HUB Santa Cruz");
            hub.setEstado("ACTIVO");
            hub = hubRepository.save(hub);

            Club club = new Club();
            club.setHub(hub);
            club.setAnfitrion(anfitrion);
            club.setNombreClub("Club Demo");
            club.setEstado("ACTIVO");
            club.setPrefijoSocio("SC");
            return clubRepository.save(club).getId();
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
}
