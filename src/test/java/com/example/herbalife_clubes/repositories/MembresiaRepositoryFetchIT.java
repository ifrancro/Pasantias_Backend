package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.dtos.membresia.MembresiaDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.NivelSocio;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.mappers.MembresiaMapper;
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
class MembresiaRepositoryFetchIT {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("membresia_fetch_it")
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
    private MembresiaRepository membresiaRepository;
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private HubRepository hubRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private NivelSocioRepository nivelSocioRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void findByIdCargaRelacionesYMapperNoLanzaLazy() {
        Integer membresiaId = seedMembresia();

        Optional<Membresia> opt = membresiaRepository.findById(membresiaId);
        assertTrue(opt.isPresent());
        Membresia membresia = opt.get();

        assertTrue(Hibernate.isInitialized(membresia.getUsuario()));
        assertTrue(Hibernate.isInitialized(membresia.getClub()));
        assertTrue(Hibernate.isInitialized(membresia.getNivel()));

        MembresiaDTO dto = assertDoesNotThrow(() -> MembresiaMapper.mapMembresiaToMembresiaDTO(membresia));
        assertEquals("Socio Test", dto.getUsuarioNombre());
        assertEquals("Club Test", dto.getClubNombre());
        assertEquals("Nivel Test", dto.getNivelNombre());
    }

    private Integer seedMembresia() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rol = new Rol();
            rol.setNombre("SOCIO-" + System.nanoTime());
            rol = rolRepository.save(rol);

            Usuario socio = new Usuario();
            socio.setRol(rol);
            socio.setNombre("Socio");
            socio.setApellido("Test");
            socio.setEmail("socio-" + System.nanoTime() + "@test.com");
            socio.setPasswordHash("x");
            socio.setEstado("ACTIVO");
            socio = usuarioRepository.save(socio);

            Hub hub = new Hub();
            hub.setAdmin(socio);
            hub.setNombre("HUB Test");
            hub.setEstado("ACTIVO");
            hub = hubRepository.save(hub);

            Club club = new Club();
            club.setHub(hub);
            club.setAnfitrion(socio);
            club.setNombreClub("Club Test");
            club.setEstado("ACTIVO");
            club.setPrefijoSocio("TE");
            club = clubRepository.save(club);

            NivelSocio nivel = new NivelSocio();
            nivel.setNombre("Nivel Test");
            nivel = nivelSocioRepository.save(nivel);

            Membresia m = new Membresia();
            m.setUsuario(socio);
            m.setClub(club);
            m.setNivel(nivel);
            m.setEstado("ACTIVA");
            return membresiaRepository.save(m).getId();
        });
    }
}
