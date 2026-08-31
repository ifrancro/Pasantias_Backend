package com.example.herbalife_clubes.exceptions;

import com.example.herbalife_clubes.dtos.pedido.PedidoComboComponenteRequestDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoComboRequestDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoConItemsDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemOpcionResponseDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Combo;
import com.example.herbalife_clubes.entities.ComboItem;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.ProductoGrupoOpcion;
import com.example.herbalife_clubes.entities.ProductoOpcion;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.pedidos.OrderCreationRejections;
import com.example.herbalife_clubes.pedidos.PedidoComboSupport;
import com.example.herbalife_clubes.pedidos.PedidoItemOpcionesSupport;
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.ComboRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.NotificacionRepository;
import com.example.herbalife_clubes.repositories.PedidoRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.PedidoServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ORD-SYNC-002: códigos estables en rechazos de POST /api/pedidos/con-items.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderCreationErrorResponseTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock private PedidoRepository pedidoRepository;
    @Mock private MembresiaRepository membresiaRepository;
    @Mock private ClubRepository clubRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ClubProductoRepository clubProductoRepository;
    @Mock private NotificacionRepository notificacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ComboRepository comboRepository;
    @Mock private PedidoComboSupport pedidoComboSupport;

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handlerMembershipInactive400() {
        assertHandler(OrderCreationRejections.membershipInactive("La membresía no está activa"),
                HttpStatus.BAD_REQUEST, OrderCreationRejections.MEMBERSHIP_INACTIVE);
    }

    @Test
    void handlerMembershipUnavailable404() {
        assertHandler(OrderCreationRejections.membershipUnavailable("Membresía no encontrada"),
                HttpStatus.NOT_FOUND, OrderCreationRejections.MEMBERSHIP_UNAVAILABLE);
    }

    @Test
    void handlerClubInactive400() {
        assertHandler(OrderCreationRejections.clubInactive("Club inactivo"),
                HttpStatus.BAD_REQUEST, OrderCreationRejections.CLUB_INACTIVE);
    }

    @Test
    void handlerClubUnavailable404() {
        assertHandler(OrderCreationRejections.clubUnavailable("Club no encontrado"),
                HttpStatus.NOT_FOUND, OrderCreationRejections.CLUB_UNAVAILABLE);
    }

    @Test
    void handlerOrderProductUnavailable() {
        assertHandler(OrderCreationRejections.productUnavailable("Producto no disponible"),
                HttpStatus.BAD_REQUEST, OrderCreationRejections.ORDER_PRODUCT_UNAVAILABLE);
    }

    @Test
    void handlerOrderComboUnavailable() {
        assertHandler(OrderCreationRejections.comboUnavailable("Combo no disponible"),
                HttpStatus.BAD_REQUEST, OrderCreationRejections.ORDER_COMBO_UNAVAILABLE);
    }

    @Test
    void handlerOrderOptionInvalid() {
        assertHandler(OrderCreationRejections.optionInvalid("Opción inválida"),
                HttpStatus.BAD_REQUEST, OrderCreationRejections.ORDER_OPTION_INVALID);
    }

    @Test
    void handlerOrderInvalidQuantity() {
        assertHandler(OrderCreationRejections.invalidQuantity("Cantidad inválida"),
                HttpStatus.BAD_REQUEST, OrderCreationRejections.ORDER_INVALID_QUANTITY);
    }

    @Test
    void handlerOrderInvalidRequest() {
        assertHandler(OrderCreationRejections.invalidRequest("Pedido vacío"),
                HttpStatus.BAD_REQUEST, OrderCreationRejections.ORDER_INVALID_REQUEST);
    }

    @Test
    void handlerOrderClientIdConflict409() {
        assertHandler(OrderCreationRejections.clientOrderIdConflict("UUID en otra membresía"),
                HttpStatus.CONFLICT, OrderCreationRejections.ORDER_CLIENT_ID_CONFLICT);
    }

    @Test
    void dataIntegrityRaceMantieneConflictRetryable() {
        DataIntegrityViolationException dive = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uq_pedidos_client_order_id\"");

        ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrity(dive);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("CONFLICT", body.get("error"));
        assertNotEquals(OrderCreationRejections.ORDER_CLIENT_ID_CONFLICT, body.get("error"));
    }

    @Test
    void membresiaInactiva400() {
        stubAuthenticatedSocio();
        Membresia membresia = membresiaActiva();
        membresia.setEstado("SUSPENDIDA");
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresia));
        when(clubRepository.findById(3)).thenReturn(Optional.of(clubActivo()));

        assertServiceRejects(
                requestConItem(10, 1),
                HttpStatus.BAD_REQUEST,
                OrderCreationRejections.MEMBERSHIP_INACTIVE);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void membresiaInexistente404() {
        stubAuthenticatedSocio();
        when(membresiaRepository.findById(1)).thenReturn(Optional.empty());

        assertServiceRejects(
                requestConItem(10, 1),
                HttpStatus.NOT_FOUND,
                OrderCreationRejections.MEMBERSHIP_UNAVAILABLE);
    }

    @Test
    void clubInactivo400() {
        stubAuthenticatedSocio();
        Membresia membresia = membresiaActiva();
        Club club = clubActivo();
        club.setEstado("PENDIENTE");
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresia));
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));

        assertServiceRejects(
                requestConItem(10, 1),
                HttpStatus.BAD_REQUEST,
                OrderCreationRejections.CLUB_INACTIVE);
    }

    @Test
    void clubInexistente404() {
        stubAuthenticatedSocio();
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresiaActiva()));
        when(clubRepository.findById(3)).thenReturn(Optional.empty());

        assertServiceRejects(
                requestConItem(10, 1),
                HttpStatus.NOT_FOUND,
                OrderCreationRejections.CLUB_UNAVAILABLE);
    }

    @Test
    void productoInexistente404() {
        stubAuthenticatedSocio();
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresiaActiva()));
        when(clubRepository.findById(3)).thenReturn(Optional.of(clubActivo()));
        when(productoRepository.findById(10)).thenReturn(Optional.empty());

        assertServiceRejects(
                requestConItem(10, 1),
                HttpStatus.NOT_FOUND,
                OrderCreationRejections.ORDER_PRODUCT_UNAVAILABLE);
    }

    @Test
    void productoInactivo400() {
        stubAuthenticatedSocio();
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresiaActiva()));
        when(clubRepository.findById(3)).thenReturn(Optional.of(clubActivo()));
        Producto producto = productoBase(10);
        producto.setActivo(false);
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));

        assertServiceRejects(
                requestConItem(10, 1),
                HttpStatus.BAD_REQUEST,
                OrderCreationRejections.ORDER_PRODUCT_UNAVAILABLE);
    }

    @Test
    void comboInactivo400() {
        ComboRepository repo = comboRepository;
        ClubProductoRepository cpRepo = clubProductoRepository;
        PedidoComboSupport support = new PedidoComboSupport(repo, cpRepo);
        Combo combo = comboBase();
        combo.setActivo(false);
        when(repo.findByIdWithItems(4)).thenReturn(Optional.of(combo));

        Pedido pedido = new Pedido();
        assertRejectsSupport(
                () -> support.materializar(pedido, comboRequest(4, 1), 3),
                HttpStatus.BAD_REQUEST,
                OrderCreationRejections.ORDER_COMBO_UNAVAILABLE);
    }

    @Test
    void comboInexistente404() {
        ComboRepository repo = comboRepository;
        ClubProductoRepository cpRepo = clubProductoRepository;
        PedidoComboSupport support = new PedidoComboSupport(repo, cpRepo);
        when(repo.findByIdWithItems(4)).thenReturn(Optional.empty());

        Pedido pedido = new Pedido();
        assertRejectsSupport(
                () -> support.materializar(pedido, comboRequest(4, 1), 3),
                HttpStatus.NOT_FOUND,
                OrderCreationRejections.ORDER_COMBO_UNAVAILABLE);
    }

    @Test
    void opcionInactiva400() {
        stubAuthenticatedSocio();
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresiaActiva()));
        when(clubRepository.findById(3)).thenReturn(Optional.of(clubActivo()));
        Producto producto = productoConOpciones();
        opcion(producto, 3, 7).setActivo(false);
        when(productoRepository.findById(7)).thenReturn(Optional.of(producto));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 7))
                .thenReturn(Optional.of(clubProductoDisponible()));

        PedidoConItemsDTO request = requestConItem(7, 1);
        request.getItems().get(0).setOpciones(List.of(sel(3, 6, 1), sel(3, 7, 1), sel(4, 9, 1)));

        assertServiceRejects(request, HttpStatus.BAD_REQUEST, OrderCreationRejections.ORDER_OPTION_INVALID);
    }

    @Test
    void minMaxOpcionesInvalido400() {
        Producto producto = productoConOpciones();
        assertRejectsSupport(
                () -> PedidoItemOpcionesSupport.validarYMaterializar(producto, List.of(sel(4, 9, 1))),
                HttpStatus.BAD_REQUEST,
                OrderCreationRejections.ORDER_OPTION_INVALID);
    }

    @Test
    void itemSueltoCantidadCero400NoPersiste() {
        stubAuthenticatedSocio();
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresiaActiva()));
        when(clubRepository.findById(3)).thenReturn(Optional.of(clubActivo()));
        when(productoRepository.findById(10)).thenReturn(Optional.of(productoBase(10)));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10))
                .thenReturn(Optional.of(clubProductoDisponible()));

        assertServiceRejects(
                requestConItem(10, 0),
                HttpStatus.BAD_REQUEST,
                OrderCreationRejections.ORDER_INVALID_QUANTITY);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void itemSueltoCantidadNegativa400() {
        stubAuthenticatedSocio();
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresiaActiva()));
        when(clubRepository.findById(3)).thenReturn(Optional.of(clubActivo()));
        when(productoRepository.findById(10)).thenReturn(Optional.of(productoBase(10)));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 10))
                .thenReturn(Optional.of(clubProductoDisponible()));

        assertServiceRejects(
                requestConItem(10, -2),
                HttpStatus.BAD_REQUEST,
                OrderCreationRejections.ORDER_INVALID_QUANTITY);
    }

    @Test
    void comboCantidadCero400() {
        ComboRepository repo = comboRepository;
        ClubProductoRepository cpRepo = clubProductoRepository;
        PedidoComboSupport support = new PedidoComboSupport(repo, cpRepo);

        Pedido pedido = new Pedido();
        PedidoComboRequestDTO request = new PedidoComboRequestDTO();
        request.setComboId(4);
        request.setCantidad(0);
        request.setComponentes(List.of());

        assertRejectsSupport(
                () -> support.materializar(pedido, request, 3),
                HttpStatus.BAD_REQUEST,
                OrderCreationRejections.ORDER_INVALID_QUANTITY);
    }

    @Test
    void pedidoVacio400() {
        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setTipoConsumo("EN_LUGAR");

        assertServiceRejects(
                request,
                HttpStatus.BAD_REQUEST,
                OrderCreationRejections.ORDER_INVALID_REQUEST);
    }

    @Test
    void clientOrderIdInvalido400() {
        PedidoConItemsDTO request = requestConItem(10, 1);
        request.setClientOrderId("no-es-un-uuid");

        assertServiceRejects(
                request,
                HttpStatus.BAD_REQUEST,
                OrderCreationRejections.ORDER_INVALID_REQUEST);
    }

    @Test
    void clientOrderIdOtraMembresia409() {
        stubAuthenticatedSocio();
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresiaActiva()));

        String clientOrderId = UUID.randomUUID().toString();
        Pedido existing = new Pedido();
        Membresia otra = new Membresia();
        otra.setId(99);
        existing.setMembresia(otra);
        existing.setClub(clubActivo());
        when(pedidoRepository.findByClientOrderId(clientOrderId)).thenReturn(Optional.of(existing));

        PedidoConItemsDTO request = requestConItem(10, 1);
        request.setClientOrderId(clientOrderId);

        assertServiceRejects(
                request,
                HttpStatus.CONFLICT,
                OrderCreationRejections.ORDER_CLIENT_ID_CONFLICT);
    }

    @Test
    void retryLegitimoMismoClientOrderIdSinRegresion() {
        stubAuthenticatedSocio();
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresiaActiva()));

        String clientOrderId = UUID.randomUUID().toString();
        Pedido existing = new Pedido();
        existing.setId(50);
        existing.setMembresia(membresiaActiva());
        existing.setClub(clubActivo());
        when(pedidoRepository.findByClientOrderId(clientOrderId)).thenReturn(Optional.of(existing));

        PedidoConItemsDTO request = requestConItem(10, 1);
        request.setClientOrderId(clientOrderId);

        var dto = pedidoService.createPedidoConItems(request, 1, 3);
        assertEquals(50, dto.getId());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void ownership403NoSeConvierteACodigoCatalogo() {
        Membresia ajena = membresiaActiva();
        Usuario otro = new Usuario();
        otro.setId(99);
        ajena.setUsuario(otro);
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(ajena));
        stubAuthenticatedSocio();

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> pedidoService.createPedidoConItems(requestConItem(10, 1), 1, 3));
        assertEquals("No tienes permisos para usar esta membresía.", ex.getMessage());
        assertNotEquals(OrderCreationRejectedException.class, ex.getClass());
    }

    private void assertHandler(OrderCreationRejectedException ex, HttpStatus status, String errorCode) {
        ResponseEntity<Map<String, Object>> response = handler.handleOrderCreationRejected(ex);
        assertEquals(status, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals(errorCode, body.get("error"));
        assertEquals(ex.getMessage(), body.get("message"));
    }

    private void assertServiceRejects(PedidoConItemsDTO request, HttpStatus status, String errorCode) {
        OrderCreationRejectedException ex = assertThrows(OrderCreationRejectedException.class,
                () -> pedidoService.createPedidoConItems(request, 1, 3));
        assertEquals(errorCode, ex.getErrorCode());
        assertEquals(status, ex.getHttpStatus());
        assertHandler(ex, status, errorCode);
    }

    private void assertRejectsSupport(org.junit.jupiter.api.function.Executable exec,
                                      HttpStatus status, String errorCode) {
        OrderCreationRejectedException ex = assertThrows(OrderCreationRejectedException.class, exec);
        assertEquals(errorCode, ex.getErrorCode());
        assertEquals(status, ex.getHttpStatus());
    }

    private void stubAuthenticatedSocio() {
        Usuario socio = new Usuario();
        socio.setId(5);
        socio.setEmail("socio@test.com");
        when(usuarioRepository.findByEmail("socio@test.com")).thenReturn(Optional.of(socio));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("socio@test.com", "n/a", Collections.emptyList()));
    }

    private static Membresia membresiaActiva() {
        Membresia membresia = new Membresia();
        membresia.setId(1);
        membresia.setEstado("ACTIVA");
        membresia.setNumeroSocio("SC-1");
        Usuario socio = new Usuario();
        socio.setId(5);
        membresia.setUsuario(socio);
        membresia.setClub(clubActivo());
        return membresia;
    }

    private static Club clubActivo() {
        Club club = new Club();
        club.setId(3);
        club.setEstado("ACTIVO");
        club.setNombreClub("Club");
        Hub hub = new Hub();
        hub.setId(1);
        club.setHub(hub);
        Usuario anfitrion = new Usuario();
        anfitrion.setId(20);
        club.setAnfitrion(anfitrion);
        return club;
    }

    private static Producto productoBase(int id) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre("Producto " + id);
        p.setEstadoAprobacion("APROBADO");
        p.setActivo(true);
        p.setPrecio(BigDecimal.TEN);
        return p;
    }

    private static ClubProducto clubProductoDisponible() {
        ClubProducto cp = new ClubProducto();
        cp.setDisponible(true);
        cp.setPrecioVenta(BigDecimal.TEN);
        return cp;
    }

    private static PedidoConItemsDTO requestConItem(int productoId, int cantidad) {
        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setTipoConsumo("EN_LUGAR");
        PedidoItemDTO item = new PedidoItemDTO();
        item.setProductoId(productoId);
        item.setCantidad(cantidad);
        request.setItems(List.of(item));
        return request;
    }

    private static PedidoComboRequestDTO comboRequest(int comboId, int cantidad) {
        PedidoComboRequestDTO combo = new PedidoComboRequestDTO();
        combo.setComboId(comboId);
        combo.setCantidad(cantidad);
        PedidoComboComponenteRequestDTO c1 = new PedidoComboComponenteRequestDTO();
        c1.setProductoId(7);
        PedidoComboComponenteRequestDTO c2 = new PedidoComboComponenteRequestDTO();
        c2.setProductoId(2);
        combo.setComponentes(List.of(c1, c2));
        return combo;
    }

    private static void stubClubProductos(ClubProductoRepository cpRepo) {
        when(cpRepo.findByClubIdAndProductoId(org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Optional.of(clubProductoDisponible()));
    }

    private static PedidoConItemsDTO requestConCombo(int comboId, int cantidad) {
        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setTipoConsumo("EN_LUGAR");
        PedidoComboRequestDTO combo = new PedidoComboRequestDTO();
        combo.setComboId(comboId);
        combo.setCantidad(cantidad);
        PedidoComboComponenteRequestDTO c1 = new PedidoComboComponenteRequestDTO();
        c1.setProductoId(7);
        PedidoComboComponenteRequestDTO c2 = new PedidoComboComponenteRequestDTO();
        c2.setProductoId(2);
        combo.setComponentes(List.of(c1, c2));
        request.setCombos(List.of(combo));
        return request;
    }

    private static Combo comboBase() {
        Club club = clubActivo();
        Producto p1 = productoBase(7);
        Producto p2 = productoBase(2);
        Combo combo = new Combo();
        combo.setId(4);
        combo.setClub(club);
        combo.setActivo(true);
        combo.setPrecio(BigDecimal.valueOf(38));
        ComboItem ci1 = new ComboItem();
        ci1.setProducto(p1);
        ci1.setCantidad(1);
        ComboItem ci2 = new ComboItem();
        ci2.setProducto(p2);
        ci2.setCantidad(1);
        combo.setItems(new ArrayList<>(List.of(ci1, ci2)));
        return combo;
    }

    private static Producto productoConOpciones() {
        Producto producto = productoBase(7);
        ProductoGrupoOpcion sabores = grupo(producto, 3, "Sabores", 0, 1, 2, true);
        sabores.getOpciones().add(opcion(sabores, 6, "Frutilla"));
        sabores.getOpciones().add(opcion(sabores, 7, "Cookies"));
        ProductoGrupoOpcion consistencia = grupo(producto, 4, "Consistencia", 1, 1, 1, false);
        consistencia.getOpciones().add(opcion(consistencia, 9, "Cremoso"));
        producto.setGruposOpciones(new ArrayList<>(List.of(sabores, consistencia)));
        return producto;
    }

    private static ProductoGrupoOpcion grupo(
            Producto producto, int id, String nombre, int orden, int min, Integer max, boolean repeat) {
        ProductoGrupoOpcion g = new ProductoGrupoOpcion();
        g.setId(id);
        g.setProducto(producto);
        g.setNombre(nombre);
        g.setOrden(orden);
        g.setMinSelecciones(min);
        g.setMaxSelecciones(max);
        g.setPermiteRepetir(repeat);
        g.setOpciones(new ArrayList<>());
        return g;
    }

    private static ProductoOpcion opcion(ProductoGrupoOpcion grupo, int id, String nombre) {
        ProductoOpcion o = new ProductoOpcion();
        o.setId(id);
        o.setGrupo(grupo);
        o.setNombre(nombre);
        o.setActivo(true);
        return o;
    }

    private static ProductoOpcion opcion(Producto producto, int grupoId, int opcionId) {
        return producto.getGruposOpciones().stream()
                .filter(g -> g.getId() == grupoId)
                .flatMap(g -> g.getOpciones().stream())
                .filter(o -> o.getId() == opcionId)
                .findFirst()
                .orElseThrow();
    }

    private static PedidoItemOpcionResponseDTO sel(int grupoId, int opcionId, int cantidad) {
        PedidoItemOpcionResponseDTO dto = new PedidoItemOpcionResponseDTO();
        dto.setGrupoId(grupoId);
        dto.setOpcionId(opcionId);
        dto.setCantidad(cantidad);
        return dto;
    }
}
