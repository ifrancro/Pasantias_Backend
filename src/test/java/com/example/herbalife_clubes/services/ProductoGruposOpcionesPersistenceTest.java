package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoGrupoOpcionDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoOpcionDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.ProductoGrupoOpcion;
import com.example.herbalife_clubes.entities.ProductoOpcion;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.ProductoServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Persistencia real de reemplazo de grupos/opciones con UNIQUE por nombre.
 * PostgreSQL vía Testcontainers (mismo patrón que {@code PedidoPaginationIT}).
 * Nombre *Test para que Surefire lo ejecute en {@code ./mvnw test}.
 * Sin Docker los 6 casos quedan skipped. No usa localhost ni credenciales locales.
 *
 * UNIQUE de entidades alineado con V18. ddl-auto=create-drop, Flyway off:
 * igual que el resto de DataJpaTest portables del proyecto.
 *
 * Deuda futura (no implementar aquí): pedido_item_opciones y club_producto_opciones
 * exigirán update por IDs estables, no reemplazo ciego que regenera filas.
 */
@Tag("postgres")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.open-in-view=false",
        "spring.jpa.show-sql=false"
})
@Testcontainers(disabledWithoutDocker = true)
@EnabledIf("dockerAvailable")
@Import(ProductoServiceImpl.class)
class ProductoGruposOpcionesPersistenceTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("producto_opciones_tc")
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
    private ProductoService productoService;
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
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void putReemplazoConMismosNombresNoViolaUnique() {
        Seeded seeded = seedProductoConSabores();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        ProductoDTO request = basePut(seeded);
        request.setGruposOpciones(List.of(
                grupo("Sabores", 0, opcion("Frutilla", 0), opcion("Chocolate", 1))));

        ProductoDTO dto = assertDoesNotThrow(
                () -> productoService.updateProducto(seeded.productoId, request, seeded.hostId),
                "Reemplazo con el mismo nombre de grupo/opción no debe violar UNIQUE");

        assertEquals(1, dto.getGruposOpciones().size());
        assertEquals("Sabores", dto.getGruposOpciones().get(0).getNombre());
        assertEquals(List.of("Frutilla", "Chocolate"),
                dto.getGruposOpciones().get(0).getOpciones().stream()
                        .map(ProductoOpcionDTO::getNombre).toList());

        tx.executeWithoutResult(status -> {
            assertEquals(1L, countGrupos(seeded.productoId));
            assertEquals(2L, countOpciones(seeded.productoId));
            assertEquals(List.of("Chocolate", "Frutilla"), nombresOpcionesOrdenados(seeded.productoId));
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void putListaVaciaEliminaGruposEnBd() {
        Seeded seeded = seedProductoConSabores();
        ProductoDTO request = basePut(seeded);
        request.setGruposOpciones(List.of());

        ProductoDTO dto = productoService.updateProducto(seeded.productoId, request, seeded.hostId);

        assertTrue(dto.getGruposOpciones().isEmpty());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertEquals(0L, countGrupos(seeded.productoId));
            assertEquals(0L, countOpciones(seeded.productoId));
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void putNullPreservaGruposEIds() {
        Seeded seeded = seedProductoConSabores();
        Integer grupoId = seeded.grupoId;
        List<Integer> opcionIds = seeded.opcionIds;

        ProductoDTO request = basePut(seeded);
        request.setGruposOpciones(null);

        ProductoDTO dto = productoService.updateProducto(seeded.productoId, request, seeded.hostId);

        assertEquals(1, dto.getGruposOpciones().size());
        assertEquals(grupoId, dto.getGruposOpciones().get(0).getId());
        assertEquals(opcionIds,
                dto.getGruposOpciones().get(0).getOpciones().stream()
                        .map(ProductoOpcionDTO::getId).toList());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertEquals(1L, countGrupos(seeded.productoId));
            assertEquals(grupoId, grupoIdPersistido(seeded.productoId));
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void reemplazoSucesivoConMismosNombresFuncionaDosVeces() {
        Seeded seeded = seedProductoConSabores();
        ProductoDTO primera = basePut(seeded);
        primera.setGruposOpciones(List.of(
                grupo("Sabores", 0, opcion("Frutilla", 0), opcion("Chocolate", 1))));
        assertDoesNotThrow(() -> productoService.updateProducto(seeded.productoId, primera, seeded.hostId));

        ProductoDTO segunda = basePut(seeded);
        segunda.setGruposOpciones(List.of(
                grupo("Sabores", 0, opcion("Frutilla", 0), opcion("Chocolate", 1))));
        ProductoDTO dto = assertDoesNotThrow(
                () -> productoService.updateProducto(seeded.productoId, segunda, seeded.hostId));

        assertEquals(1, dto.getGruposOpciones().size());
        assertEquals(List.of("Frutilla", "Chocolate"),
                dto.getGruposOpciones().get(0).getOpciones().stream()
                        .map(ProductoOpcionDTO::getNombre).toList());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertEquals(1L, countGrupos(seeded.productoId));
            assertEquals(2L, countOpciones(seeded.productoId));
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void ordenDeGruposYOpcionesPersiste() {
        Seeded seeded = seedProductoConSabores();
        ProductoDTO request = basePut(seeded);
        request.setGruposOpciones(List.of(
                grupo("Consistencia", 0, opcion("Líquido", 1), opcion("Cremoso", 0)),
                grupo("Sabores", 1, opcion("Vainilla", 0), opcion("Frutilla", 1))));

        ProductoDTO dto = productoService.updateProducto(seeded.productoId, request, seeded.hostId);

        assertEquals(List.of("Consistencia", "Sabores"),
                dto.getGruposOpciones().stream().map(ProductoGrupoOpcionDTO::getNombre).toList());
        assertEquals(List.of("Cremoso", "Líquido"),
                dto.getGruposOpciones().get(0).getOpciones().stream()
                        .map(ProductoOpcionDTO::getNombre).toList());
        assertEquals(List.of("Vainilla", "Frutilla"),
                dto.getGruposOpciones().get(1).getOpciones().stream()
                        .map(ProductoOpcionDTO::getNombre).toList());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertEquals(2L, countGrupos(seeded.productoId));
            @SuppressWarnings("unchecked")
            List<String> nombres = entityManager.createNativeQuery(
                            "SELECT g.nombre FROM producto_grupos_opciones g "
                                    + "WHERE g.producto_id = :pid ORDER BY g.orden, g.id")
                    .setParameter("pid", seeded.productoId)
                    .getResultList();
            assertEquals(List.of("Consistencia", "Sabores"), nombres);
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void flushNoDejaColeccionDuplicada() {
        Seeded seeded = seedProductoConSabores();
        ProductoDTO request = basePut(seeded);
        request.setGruposOpciones(List.of(
                grupo("Sabores", 0, opcion("Frutilla", 0), opcion("Chocolate", 1))));

        assertDoesNotThrow(() -> productoService.updateProducto(seeded.productoId, request, seeded.hostId));
        assertDoesNotThrow(() -> productoService.updateProducto(seeded.productoId, request, seeded.hostId));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertEquals(1L, countGrupos(seeded.productoId));
            assertEquals(2L, countOpciones(seeded.productoId));
        });
    }

    private Seeded seedProductoConSabores() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rol = new Rol();
            rol.setNombre("ANFITRION");
            rol = rolRepository.save(rol);

            Usuario host = new Usuario();
            host.setRol(rol);
            host.setNombre("Andrea");
            host.setApellido("Host");
            host.setEmail("host-" + System.nanoTime() + "@club.com");
            host.setPasswordHash("x");
            host.setEstado("ACTIVO");
            host = usuarioRepository.save(host);

            Hub hub = new Hub();
            hub.setAdmin(host);
            hub.setNombre("HUB IT");
            hub.setEstado("ACTIVO");
            hub = hubRepository.save(hub);

            Club club = new Club();
            club.setHub(hub);
            club.setAnfitrion(host);
            club.setNombreClub("Club IT");
            club.setEstado("ACTIVO");
            club.setPrefijoSocio("IT");
            club = clubRepository.save(club);

            Producto producto = new Producto();
            producto.setHub(hub);
            producto.setClubCreador(club);
            producto.setNombre("Batido");
            producto.setDescripcion("Proteico");
            producto.setIngredientes("proteína");
            producto.setPuntosValor(10);
            producto.setPrecio(BigDecimal.ZERO);
            producto.setTipo("LOCAL");
            producto.setEstadoAprobacion("PENDIENTE");
            producto.setActivo(true);
            producto.setGruposOpciones(new ArrayList<>());

            ProductoGrupoOpcion grupo = new ProductoGrupoOpcion();
            grupo.setProducto(producto);
            grupo.setNombre("Sabores");
            grupo.setOrden(0);
            grupo.setMinSelecciones(1);
            grupo.setMaxSelecciones(2);
            grupo.setPermiteRepetir(true);
            grupo.setOpciones(new ArrayList<>());
            grupo.getOpciones().add(opcionEntity(grupo, "Frutilla", 0));
            grupo.getOpciones().add(opcionEntity(grupo, "Vainilla", 1));
            producto.getGruposOpciones().add(grupo);

            producto = productoRepository.saveAndFlush(producto);
            ProductoGrupoOpcion savedGrupo = producto.getGruposOpciones().get(0);
            return new Seeded(
                    host.getId(),
                    producto.getId(),
                    savedGrupo.getId(),
                    savedGrupo.getOpciones().stream().map(ProductoOpcion::getId).toList(),
                    producto.getNombre(),
                    producto.getDescripcion(),
                    producto.getIngredientes(),
                    producto.getPuntosValor());
        });
    }

    private ProductoDTO basePut(Seeded seeded) {
        ProductoDTO request = new ProductoDTO();
        request.setNombre(seeded.nombre);
        request.setDescripcion(seeded.descripcion);
        request.setIngredientes(seeded.ingredientes);
        request.setPuntosValor(seeded.puntosValor);
        request.setPrecio(BigDecimal.ZERO);
        return request;
    }

    private static ProductoGrupoOpcionDTO grupo(String nombre, int orden, ProductoOpcionDTO... opciones) {
        ProductoGrupoOpcionDTO dto = new ProductoGrupoOpcionDTO();
        dto.setNombre(nombre);
        dto.setOrden(orden);
        dto.setMinSelecciones(1);
        dto.setMaxSelecciones(2);
        dto.setPermiteRepetir(true);
        dto.setOpciones(List.of(opciones));
        return dto;
    }

    private static ProductoOpcionDTO opcion(String nombre, int orden) {
        ProductoOpcionDTO dto = new ProductoOpcionDTO();
        dto.setNombre(nombre);
        dto.setOrden(orden);
        return dto;
    }

    private static ProductoOpcion opcionEntity(ProductoGrupoOpcion grupo, String nombre, int orden) {
        ProductoOpcion opcion = new ProductoOpcion();
        opcion.setGrupo(grupo);
        opcion.setNombre(nombre);
        opcion.setOrden(orden);
        opcion.setActivo(true);
        return opcion;
    }

    private long countGrupos(Integer productoId) {
        return ((Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM producto_grupos_opciones WHERE producto_id = :pid")
                .setParameter("pid", productoId)
                .getSingleResult()).longValue();
    }

    private long countOpciones(Integer productoId) {
        return ((Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM producto_opciones o "
                                + "JOIN producto_grupos_opciones g ON g.id = o.grupo_id "
                                + "WHERE g.producto_id = :pid")
                .setParameter("pid", productoId)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<String> nombresOpcionesOrdenados(Integer productoId) {
        return entityManager.createNativeQuery(
                        "SELECT o.nombre FROM producto_opciones o "
                                + "JOIN producto_grupos_opciones g ON g.id = o.grupo_id "
                                + "WHERE g.producto_id = :pid ORDER BY o.nombre")
                .setParameter("pid", productoId)
                .getResultList();
    }

    private Integer grupoIdPersistido(Integer productoId) {
        Number id = (Number) entityManager.createNativeQuery(
                        "SELECT id FROM producto_grupos_opciones WHERE producto_id = :pid")
                .setParameter("pid", productoId)
                .getSingleResult();
        return id.intValue();
    }

    private record Seeded(
            Integer hostId,
            Integer productoId,
            Integer grupoId,
            List<Integer> opcionIds,
            String nombre,
            String descripcion,
            String ingredientes,
            Integer puntosValor) {
    }
}
