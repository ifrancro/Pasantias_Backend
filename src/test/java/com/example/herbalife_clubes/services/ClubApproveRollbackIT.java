package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.notificacion.NotificacionDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.ClubServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CLUB-002: si la notificación falla, club y rol no deben quedar a medias.
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
@Import({ClubServiceImpl.class, ClubApproveRollbackIT.FailingNotificacionConfig.class})
class ClubApproveRollbackIT {

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

    @TestConfiguration
    static class FailingNotificacionConfig {
        @Bean
        @Primary
        NotificacionService failingNotificacionService() {
            return new NotificacionService() {
                @Override
                public NotificacionDTO enviarNotificacion(NotificacionDTO notificacionDTO, Integer hubId,
                                                          Integer clubId, Integer usuarioId, Integer pedidoId) {
                    throw new RuntimeException("fallo intencional de notificacion");
                }

                @Override
                public List<NotificacionDTO> getHistorialByUsuario(Integer usuarioId) {
                    return List.of();
                }

                @Override
                public List<NotificacionDTO> getHistorialByHub(Integer hubId) {
                    return List.of();
                }

                @Override
                public List<NotificacionDTO> getHistorialByClub(Integer clubId) {
                    return List.of();
                }

                @Override
                public List<NotificacionDTO> getHistorial() {
                    return List.of();
                }
            };
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
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void aprobarClubRevierteClubYRolSiFallaLaNotificacion() {
        SeededClub seeded = seedPendingClub();
        String rolAntes = usuarioRepository.findById(seeded.anfitrionId()).orElseThrow().getRol().getNombre();
        assertNotEquals("ANFITRION", rolAntes);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> clubService.aprobarClub(seeded.clubId()));
        assertEquals("fallo intencional de notificacion", ex.getMessage());

        assertEquals("PENDIENTE", clubRepository.findById(seeded.clubId()).orElseThrow().getEstado());
        assertEquals(rolAntes,
                usuarioRepository.findById(seeded.anfitrionId()).orElseThrow().getRol().getNombre());
    }

    private SeededClub seedPendingClub() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            rolRepository.findByNombre("ANFITRION").orElseGet(() -> {
                Rol rol = new Rol();
                rol.setNombre("ANFITRION");
                return rolRepository.save(rol);
            });
            Rol basico = new Rol();
            basico.setNombre("USUARIO_BASICO-" + System.nanoTime());
            basico = rolRepository.save(basico);

            Usuario admin = usuario("Admin", "Corporativo", "admin-" + System.nanoTime() + "@it.com",
                    rolRepository.findByNombre("ANFITRION").orElseThrow());
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
            club = clubRepository.save(club);
            return new SeededClub(club.getId(), host.getId());
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

    private record SeededClub(Integer clubId, Integer anfitrionId) {
    }
}
