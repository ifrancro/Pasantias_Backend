package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.common.PagedResponse;
import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Combo;
import com.example.herbalife_clubes.entities.EstadoPedido;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.PedidoItem;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.TipoConsumo;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.mappers.PedidoMapper;
import com.example.herbalife_clubes.services.PedidoService;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Casos comunes de ORD-HIST-001. Las subclases eligen PostgreSQL
 * (Testcontainers o {@code club_diag_it}).
 */
abstract class PedidoPaginationITBase {

    static final LocalDateTime FECHA_A = LocalDateTime.of(2026, 1, 10, 12, 0);
    static final LocalDateTime FECHA_B = LocalDateTime.of(2026, 1, 20, 12, 0);
    static final LocalDateTime FECHA_C = LocalDateTime.of(2026, 2, 1, 12, 0);
    static final LocalDateTime DESDE_SOLO = LocalDateTime.of(2026, 1, 15, 0, 0);
    static final LocalDateTime HASTA_SOLO = LocalDateTime.of(2026, 1, 15, 0, 0);
    static final LocalDateTime HASTA_RANGO = LocalDateTime.of(2026, 2, 1, 0, 0);

    @Autowired
    PedidoRepository pedidoRepository;
    @Autowired
    PedidoService pedidoService;
    @Autowired
    ClubRepository clubRepository;
    @Autowired
    HubRepository hubRepository;
    @Autowired
    UsuarioRepository usuarioRepository;
    @Autowired
    RolRepository rolRepository;
    @Autowired
    MembresiaRepository membresiaRepository;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    ComboRepository comboRepository;
    @Autowired
    PlatformTransactionManager transactionManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void membresiaSinPedidosYFiltrosNullNoLanza() {
        Fixture seed = seed(false);
        authenticate(seed.vacioEmail());

        PagedResponse<PedidoDTO> page = assertDoesNotThrow(() ->
                pedidoService.getPedidosBySocioPaginados(
                        seed.emptyMembresiaId(), 0, 20, null, null, null));

        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalElements());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void membresiaConPedidosYFiltrosNullDevuelveTodosOrdenados() {
        Fixture seed = seed(true);
        authenticate(seed.socioEmail());

        PagedResponse<PedidoDTO> page = assertDoesNotThrow(() ->
                pedidoService.getPedidosBySocioPaginados(
                        seed.membresiaId(), 0, 20, null, null, null));

        assertEquals(3, page.content().size());
        assertEquals(3, page.totalElements());
        assertEquals(List.of(seed.pedidoCId(), seed.pedidoBId(), seed.pedidoAId()),
                page.content().stream().map(PedidoDTO::getId).toList());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void filtraPorEstadoRecibido() {
        Fixture seed = seed(true);
        authenticate(seed.socioEmail());

        PagedResponse<PedidoDTO> page = pedidoService.getPedidosBySocioPaginados(
                seed.membresiaId(), 0, 20, "RECIBIDO", null, null);

        assertEquals(List.of(seed.pedidoAId()),
                page.content().stream().map(PedidoDTO::getId).toList());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void filtraSoloDesde() {
        Fixture seed = seed(true);
        authenticate(seed.socioEmail());

        PagedResponse<PedidoDTO> page = pedidoService.getPedidosBySocioPaginados(
                seed.membresiaId(), 0, 20, null, DESDE_SOLO, null);

        assertEquals(List.of(seed.pedidoCId(), seed.pedidoBId()),
                page.content().stream().map(PedidoDTO::getId).toList());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void filtraSoloHasta() {
        Fixture seed = seed(true);
        authenticate(seed.socioEmail());

        PagedResponse<PedidoDTO> page = pedidoService.getPedidosBySocioPaginados(
                seed.membresiaId(), 0, 20, null, null, HASTA_SOLO);

        assertEquals(List.of(seed.pedidoAId()),
                page.content().stream().map(PedidoDTO::getId).toList());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void filtraRangoDesdeYHasta() {
        Fixture seed = seed(true);
        authenticate(seed.socioEmail());

        PagedResponse<PedidoDTO> page = pedidoService.getPedidosBySocioPaginados(
                seed.membresiaId(), 0, 20, null, DESDE_SOLO, HASTA_RANGO);

        assertEquals(List.of(seed.pedidoBId()),
                page.content().stream().map(PedidoDTO::getId).toList());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void paginadoPorClubUsaLaMismaConstruccionDeFiltros() {
        Fixture seed = seed(true);
        authenticate(seed.hostEmail());

        PagedResponse<PedidoDTO> sinFiltros = assertDoesNotThrow(() ->
                pedidoService.getPedidosByClubPaginados(seed.clubId(), 0, 20, null, null, null));
        assertEquals(3, sinFiltros.totalElements());
        assertEquals(List.of(seed.pedidoCId(), seed.pedidoBId(), seed.pedidoAId()),
                sinFiltros.content().stream().map(PedidoDTO::getId).toList());

        assertEquals(1, pedidoService.getPedidosByClubPaginados(
                seed.clubId(), 0, 20, "RECIBIDO", null, null).totalElements());
        assertEquals(2, pedidoService.getPedidosByClubPaginados(
                seed.clubId(), 0, 20, null, DESDE_SOLO, null).totalElements());
        assertEquals(1, pedidoService.getPedidosByClubPaginados(
                seed.clubId(), 0, 20, null, null, HASTA_SOLO).totalElements());
        assertEquals(1, pedidoService.getPedidosByClubPaginados(
                seed.clubId(), 0, 20, null, DESDE_SOLO, HASTA_RANGO).totalElements());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void itemSinComboMapeaComboNull() {
        Fixture seed = seed(true);
        authenticate(seed.socioEmail());

        PedidoDTO dto = pedidoService.getPedidosBySocioPaginados(
                seed.membresiaId(), 0, 20, null, null, null)
                .content().stream()
                .filter(p -> seed.pedidoAId().equals(p.getId()))
                .findFirst()
                .orElseThrow();

        assertFalse(dto.getItems().isEmpty());
        PedidoItemDTO item = dto.getItems().get(0);
        assertNull(item.getComboId());
        assertNull(item.getComboNombre());
        assertNotNull(item.getProductoId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void itemConComboSeMapeaFueraDeSesionSinLazy() {
        Fixture seed = seed(true);

        Pedido detached = pedidoRepository.findWithRelationsByIds(List.of(seed.pedidoBId())).get(0);
        assertComboInicializadoSinItems(detached);

        PedidoDTO dto = assertDoesNotThrow(() -> PedidoMapper.mapPedidoToPedidoDTO(detached));
        PedidoItemDTO item = dto.getItems().get(0);
        assertEquals(seed.comboId(), item.getComboId());
        assertEquals("Shake Mañana", item.getComboNombre());

        Pedido porSocio = pedidoRepository.findByMembresiaIdWithRelations(seed.membresiaId())
                .stream()
                .filter(p -> seed.pedidoBId().equals(p.getId()))
                .findFirst()
                .orElseThrow();
        assertComboInicializadoSinItems(porSocio);
        PedidoDTO dtoSocio = assertDoesNotThrow(() -> PedidoMapper.mapPedidoToPedidoDTO(porSocio));
        assertEquals("Shake Mañana", dtoSocio.getItems().get(0).getComboNombre());

        Pedido porClub = pedidoRepository.findByClubIdWithRelations(seed.clubId())
                .stream()
                .filter(p -> seed.pedidoBId().equals(p.getId()))
                .findFirst()
                .orElseThrow();
        assertComboInicializadoSinItems(porClub);
        PedidoDTO dtoClub = assertDoesNotThrow(() -> PedidoMapper.mapPedidoToPedidoDTO(porClub));
        assertEquals(seed.comboId(), dtoClub.getItems().get(0).getComboId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Rollback(false)
    void findIdsByMembresiaIdConFlagsFalseNoUsaIsNullDeEnum() {
        Fixture seed = seed(true);
        Pageable pageable = PageRequest.of(
                0, 20, Sort.by(Sort.Order.desc("fechaPedido"), Sort.Order.desc("id")));

        Page<Integer> ids = assertDoesNotThrow(() -> pedidoRepository.findIdsByMembresiaId(
                seed.membresiaId(),
                false, EstadoPedido.RECIBIDO,
                false, LocalDateTime.of(1970, 1, 1, 0, 0),
                false, LocalDateTime.of(1970, 1, 1, 0, 0),
                pageable));

        assertEquals(3, ids.getTotalElements());
        assertEquals(List.of(seed.pedidoCId(), seed.pedidoBId(), seed.pedidoAId()), ids.getContent());
    }

    private static void assertComboInicializadoSinItems(Pedido pedido) {
        assertFalse(pedido.getItems().isEmpty());
        PedidoItem item = pedido.getItems().get(0);
        assertNotNull(item.getCombo());
        assertTrue(Hibernate.isInitialized(item.getCombo()));
        assertFalse(Hibernate.isInitialized(item.getCombo().getItems()));
    }

    private Fixture seed(boolean withPedidos) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Rol rol = new Rol();
            rol.setNombre("SOCIO-" + System.nanoTime());
            rol = rolRepository.save(rol);

            Usuario admin = usuario("Admin", "IT", rol);
            Usuario socio = usuario("Socio", "Uno", rol);
            Usuario vacio = usuario("Socio", "Vacio", rol);

            Hub hub = new Hub();
            hub.setAdmin(admin);
            hub.setNombre("HUB IT");
            hub.setEstado("ACTIVO");
            hub = hubRepository.save(hub);

            Club club = new Club();
            club.setHub(hub);
            club.setAnfitrion(admin);
            club.setNombreClub("Club IT");
            club.setEstado("ACTIVO");
            club.setPrefijoSocio("IT");
            club = clubRepository.save(club);

            Membresia membresia = membresia(socio, club);
            Membresia empty = membresia(vacio, club);

            Producto producto = new Producto();
            producto.setHub(hub);
            producto.setClubCreador(club);
            producto.setNombre("Té verde");
            producto.setActivo(true);
            producto.setPrecio(BigDecimal.TEN);
            producto = productoRepository.save(producto);

            Combo combo = new Combo();
            combo.setClub(club);
            combo.setNombre("Shake Mañana");
            combo.setActivo(true);
            combo = comboRepository.save(combo);

            Integer pedidoA = null;
            Integer pedidoB = null;
            Integer pedidoC = null;
            if (withPedidos) {
                pedidoA = savePedido(membresia, club, producto, null, EstadoPedido.RECIBIDO, FECHA_A);
                pedidoB = savePedido(membresia, club, producto, combo, EstadoPedido.ENTREGADO, FECHA_B);
                pedidoC = savePedido(membresia, club, producto, null, EstadoPedido.CANCELADO, FECHA_C);
            }

            return new Fixture(
                    club.getId(),
                    membresia.getId(),
                    empty.getId(),
                    combo.getId(),
                    pedidoA,
                    pedidoB,
                    pedidoC,
                    socio.getEmail(),
                    admin.getEmail(),
                    vacio.getEmail());
        });
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "n/a", Collections.emptyList()));
    }

    private Integer savePedido(
            Membresia membresia,
            Club club,
            Producto producto,
            Combo combo,
            EstadoPedido estado,
            LocalDateTime fecha) {
        Pedido pedido = new Pedido();
        pedido.setMembresia(membresia);
        pedido.setClub(club);
        pedido.setProducto(producto);
        pedido.setCantidad(1);
        pedido.setTipoConsumo(TipoConsumo.EN_LUGAR);
        pedido.setEstado(estado);
        pedido.setFechaPedido(fecha);

        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setProducto(producto);
        item.setCombo(combo);
        item.setCantidad(1);
        item.setPrecioUnitario(BigDecimal.TEN);
        pedido.getItems().add(item);

        return pedidoRepository.save(pedido).getId();
    }

    private Membresia membresia(Usuario usuario, Club club) {
        Membresia membresia = new Membresia();
        membresia.setUsuario(usuario);
        membresia.setClub(club);
        membresia.setNumeroSocio("SOC-" + System.nanoTime());
        membresia.setEstado("ACTIVA");
        return membresiaRepository.save(membresia);
    }

    private Usuario usuario(String nombre, String apellido, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setEmail(nombre.toLowerCase() + "-" + System.nanoTime() + "@it.com");
        usuario.setPasswordHash("x");
        usuario.setEstado("ACTIVO");
        return usuarioRepository.save(usuario);
    }

    record Fixture(
            Integer clubId,
            Integer membresiaId,
            Integer emptyMembresiaId,
            Integer comboId,
            Integer pedidoAId,
            Integer pedidoBId,
            Integer pedidoCId,
            String socioEmail,
            String hostEmail,
            String vacioEmail) {
    }
}
