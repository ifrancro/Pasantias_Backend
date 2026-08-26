package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.mappers.ProductoMapper;
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

import java.math.BigDecimal;
import java.util.List;

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
class ProductoRepositoryFetchIT {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("producto_fetch_it")
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
    private ProductoRepository productoRepository;
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
    void findAllCargaHubYClubCreadorYMapperNoLanzaLazy() {
        Integer productoId = seedProducto();

        List<Producto> productos = productoRepository.findAll();
        assertFalse(productos.isEmpty());
        Producto producto = productos.stream().filter(p -> productoId.equals(p.getId())).findFirst().orElseThrow();

        assertTrue(Hibernate.isInitialized(producto.getHub()));
        assertTrue(Hibernate.isInitialized(producto.getClubCreador()));

        ProductoDTO dto = assertDoesNotThrow(() -> ProductoMapper.mapProductoToProductoDTO(producto));
        assertEquals("HUB Test", dto.getHubNombre());
        assertEquals("Club Test", dto.getClubCreadorNombre());
    }

    private Integer seedProducto() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rol = new Rol();
            rol.setNombre("ANFITRION-" + System.nanoTime());
            rol = rolRepository.save(rol);

            Usuario admin = new Usuario();
            admin.setRol(rol);
            admin.setNombre("Admin");
            admin.setApellido("Test");
            admin.setEmail("admin-" + System.nanoTime() + "@test.com");
            admin.setPasswordHash("x");
            admin.setEstado("ACTIVO");
            admin = usuarioRepository.save(admin);

            Hub hub = new Hub();
            hub.setAdmin(admin);
            hub.setNombre("HUB Test");
            hub.setEstado("ACTIVO");
            hub = hubRepository.save(hub);

            Club club = new Club();
            club.setHub(hub);
            club.setAnfitrion(admin);
            club.setNombreClub("Club Test");
            club.setEstado("ACTIVO");
            club.setPrefijoSocio("TE");
            club = clubRepository.save(club);

            Producto p = new Producto();
            p.setHub(hub);
            p.setClubCreador(club);
            p.setNombre("Té");
            p.setActivo(true);
            p.setPrecio(BigDecimal.TEN);
            return productoRepository.save(p).getId();
        });
    }
}
