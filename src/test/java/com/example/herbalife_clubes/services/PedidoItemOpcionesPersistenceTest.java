package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.pedido.PedidoConItemsDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemOpcionResponseDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.PedidoItem;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.ProductoGrupoOpcion;
import com.example.herbalife_clubes.entities.ProductoOpcion;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.pedidos.PedidoComboSupport;
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.PedidoRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.PedidoServiceImpl;
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
class PedidoItemOpcionesPersistenceTest {

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
    @Autowired private ProductoRepository productoRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private MembresiaRepository membresiaRepository;
    @Autowired private ClubRepository clubRepository;
    @Autowired private HubRepository hubRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private ClubProductoRepository clubProductoRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void persistenciaSnapshotsYDeleteOpcionSetNull() {
        Seeded seeded = seed();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setTipoConsumo("EN_LUGAR");
        PedidoItemDTO item = new PedidoItemDTO();
        item.setProductoId(seeded.productoId);
        item.setCantidad(1);
        item.setOpciones(List.of(sel(seeded.grupoId, seeded.opcionId, 1)));
        request.setItems(List.of(item));

        var dto = pedidoService.createPedidoConItems(request, seeded.membresiaId, seeded.clubId);
        Integer pedidoId = dto.getId();
        assertEquals("Frutilla", dto.getItems().get(0).getOpciones().get(0).getOpcionNombre());

        tx.executeWithoutResult(status -> {
            entityManager.clear();
            ProductoOpcion opcion = entityManager.find(ProductoOpcion.class, seeded.opcionId);
            opcion.setNombre("Fresa");
            entityManager.flush();
        });

        tx.executeWithoutResult(status -> {
            entityManager.clear();
            Pedido recargado = pedidoRepository.findById(pedidoId).orElseThrow();
            assertEquals("Frutilla",
                    recargado.getItems().get(0).getOpciones().get(0).getOpcionNombreSnapshot());
        });

        tx.executeWithoutResult(status -> {
            // ddl-auto no replica ON DELETE SET NULL de V20; aplicar el mismo efecto que prod.
            entityManager.createNativeQuery(
                            "UPDATE pedido_item_opciones SET opcion_id = NULL WHERE opcion_id = :id")
                    .setParameter("id", seeded.opcionId)
                    .executeUpdate();
            entityManager.createNativeQuery("DELETE FROM producto_opciones WHERE id = :id")
                    .setParameter("id", seeded.opcionId)
                    .executeUpdate();
            entityManager.flush();
            entityManager.clear();
            Pedido recargado = pedidoRepository.findById(pedidoId).orElseThrow();
            var sel = recargado.getItems().get(0).getOpciones().get(0);
            assertNull(sel.getOpcion());
            assertEquals("Frutilla", sel.getOpcionNombreSnapshot());
        });
    }

    private Seeded seed() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rol = rolRepository.save(rol("SOCIO"));
            Usuario socio = usuarioRepository.save(usuario(rol, "socio-pio@test.com"));
            Rol rolHost = rolRepository.save(rol("ANFITRION"));
            Usuario host = usuarioRepository.save(usuario(rolHost, "host-pio@test.com"));
            Hub hub = hubRepository.save(hub(host));
            Club club = clubRepository.save(club(hub, host));

            Membresia membresia = new Membresia();
            membresia.setUsuario(socio);
            membresia.setClub(club);
            membresia.setNumeroSocio("P-1");
            membresia.setEstado("ACTIVA");
            membresia = membresiaRepository.save(membresia);

            Producto producto = new Producto();
            producto.setHub(hub);
            producto.setClubCreador(club);
            producto.setNombre("Batido");
            producto.setPrecio(BigDecimal.TEN);
            producto.setTipo("LOCAL");
            producto.setEstadoAprobacion("APROBADO");
            producto.setActivo(true);
            producto.setGruposOpciones(new ArrayList<>());

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
            producto = productoRepository.saveAndFlush(producto);

            var cp = new com.example.herbalife_clubes.entities.ClubProducto();
            cp.setClub(club);
            cp.setProducto(producto);
            cp.setDisponible(true);
            clubProductoRepository.save(cp);

            return new Seeded(
                    membresia.getId(),
                    club.getId(),
                    producto.getId(),
                    producto.getGruposOpciones().get(0).getId(),
                    frutilla.getId());
        });
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

    private static Hub hub(Usuario admin) {
        Hub h = new Hub();
        h.setAdmin(admin);
        h.setNombre("Hub");
        h.setEstado("ACTIVO");
        return h;
    }

    private static Club club(Hub hub, Usuario host) {
        Club c = new Club();
        c.setHub(hub);
        c.setAnfitrion(host);
        c.setNombreClub("Club PIO");
        c.setEstado("ACTIVO");
        c.setPrefijoSocio("P");
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

    private record Seeded(Integer membresiaId, Integer clubId, Integer productoId, Integer grupoId, Integer opcionId) {
    }
}
