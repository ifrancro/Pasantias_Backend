package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.pedido.PedidoConItemsDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemOpcionResponseDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.PedidoItem;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.ProductoGrupoOpcion;
import com.example.herbalife_clubes.entities.ProductoOpcion;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoItemOpcionesServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private MembresiaRepository membresiaRepository;
    @Mock private ClubRepository clubRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ClubProductoRepository clubProductoRepository;
    @Mock private NotificacionRepository notificacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ComboRepository comboRepository;

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pedidoConItemsPersisteOpcionesSnapshot() {
        stubSocio();
        Producto producto = productoConGrupos();
        when(productoRepository.findById(7)).thenReturn(Optional.of(producto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(100);
            return p;
        });

        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setTipoConsumo("EN_LUGAR");
        PedidoItemDTO item = new PedidoItemDTO();
        item.setProductoId(7);
        item.setCantidad(1);
        item.setOpciones(List.of(sel(3, 6, 1), sel(4, 9, 1)));
        request.setItems(List.of(item));

        PedidoDTO creado = pedidoService.createPedidoConItems(request, 1, 3);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        PedidoItem savedItem = captor.getValue().getItems().get(0);
        assertEquals(2, savedItem.getOpciones().size());
        assertEquals("Frutilla", savedItem.getOpciones().get(0).getOpcionNombreSnapshot());
        assertEquals(6, savedItem.getOpciones().get(0).getOpcion().getId());

        assertEquals(2, creado.getItems().get(0).getOpciones().size());
        assertEquals("Sabores", creado.getItems().get(0).getOpciones().get(0).getGrupoNombre());
        assertEquals("Frutilla", creado.getItems().get(0).getOpciones().get(0).getOpcionNombre());
    }

    @Test
    void dosItemsMismoProductoDistintaConfiguracion() {
        stubSocio();
        Producto producto = productoConGrupos();
        when(productoRepository.findById(7)).thenReturn(Optional.of(producto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(101);
            return p;
        });

        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setTipoConsumo("EN_LUGAR");
        PedidoItemDTO a = new PedidoItemDTO();
        a.setProductoId(7);
        a.setCantidad(1);
        a.setOpciones(List.of(sel(3, 6, 1), sel(4, 9, 1)));
        PedidoItemDTO b = new PedidoItemDTO();
        b.setProductoId(7);
        b.setCantidad(1);
        b.setOpciones(List.of(sel(3, 7, 1), sel(4, 10, 1)));
        request.setItems(List.of(a, b));

        PedidoDTO creado = pedidoService.createPedidoConItems(request, 1, 3);

        assertEquals(2, creado.getItems().size());
        assertEquals("Frutilla", creado.getItems().get(0).getOpciones().get(0).getOpcionNombre());
        assertEquals("Cookies", creado.getItems().get(1).getOpciones().get(0).getOpcionNombre());
    }

    @Test
    void itemInvalidoNoPersistePedidoParcial() {
        stubSocio();
        Producto producto = productoConGrupos();
        when(productoRepository.findById(7)).thenReturn(Optional.of(producto));

        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setTipoConsumo("EN_LUGAR");
        PedidoItemDTO ok = new PedidoItemDTO();
        ok.setProductoId(7);
        ok.setCantidad(1);
        ok.setOpciones(List.of(sel(3, 6, 1), sel(4, 9, 1)));
        PedidoItemDTO bad = new PedidoItemDTO();
        bad.setProductoId(7);
        bad.setCantidad(1);
        bad.setOpciones(List.of(sel(4, 9, 1)));
        request.setItems(List.of(ok, bad));

        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.createPedidoConItems(request, 1, 3));
    }

    @Test
    void precioNoCambiaPorOpciones() {
        stubSocio();
        Producto producto = productoConGrupos();
        producto.setPrecio(bd("25.00"));
        when(productoRepository.findById(7)).thenReturn(Optional.of(producto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoConItemsDTO request = new PedidoConItemsDTO();
        request.setTipoConsumo("EN_LUGAR");
        PedidoItemDTO item = new PedidoItemDTO();
        item.setProductoId(7);
        item.setCantidad(2);
        item.setOpciones(List.of(sel(3, 6, 1), sel(4, 9, 1)));
        request.setItems(List.of(item));

        PedidoDTO creado = pedidoService.createPedidoConItems(request, 1, 3);

        assertEquals(0, bd("25.00").compareTo(creado.getItems().get(0).getPrecioUnitario()));
        assertEquals(0, bd("50.00").compareTo(creado.getItems().get(0).getSubtotal()));
    }

    private void stubSocio() {
        Usuario socio = new Usuario();
        socio.setId(5);
        socio.setEmail("socio@test.com");
        Membresia membresia = new Membresia();
        membresia.setId(1);
        membresia.setEstado("ACTIVA");
        membresia.setNumeroSocio("SC-1");
        membresia.setUsuario(socio);
        Club club = new Club();
        club.setId(3);
        club.setEstado("ACTIVO");
        club.setNombreClub("Club");
        Hub hub = new Hub();
        hub.setId(1);
        club.setHub(hub);
        membresia.setClub(club);
        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresia));
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(usuarioRepository.findByEmail("socio@test.com")).thenReturn(Optional.of(socio));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("socio@test.com", "n/a", Collections.emptyList()));
        ClubProducto cp = new ClubProducto();
        cp.setDisponible(true);
        when(clubProductoRepository.findByClubIdAndProductoId(3, 7)).thenReturn(Optional.of(cp));
    }

    private static Producto productoConGrupos() {
        Producto producto = new Producto();
        producto.setId(7);
        producto.setNombre("Batido");
        producto.setEstadoAprobacion("APROBADO");
        producto.setActivo(true);
        producto.setPrecio(bd("25.00"));
        producto.setGruposOpciones(new ArrayList<>());

        ProductoGrupoOpcion sabores = new ProductoGrupoOpcion();
        sabores.setId(3);
        sabores.setProducto(producto);
        sabores.setNombre("Sabores");
        sabores.setOrden(0);
        sabores.setMinSelecciones(1);
        sabores.setMaxSelecciones(2);
        sabores.setPermiteRepetir(true);
        sabores.setOpciones(new ArrayList<>());
        ProductoOpcion frutilla = opcion(sabores, 6, "Frutilla", 0);
        ProductoOpcion cookies = opcion(sabores, 7, "Cookies", 1);
        sabores.getOpciones().add(frutilla);
        sabores.getOpciones().add(cookies);

        ProductoGrupoOpcion consistencia = new ProductoGrupoOpcion();
        consistencia.setId(4);
        consistencia.setProducto(producto);
        consistencia.setNombre("Consistencia");
        consistencia.setOrden(1);
        consistencia.setMinSelecciones(1);
        consistencia.setMaxSelecciones(1);
        consistencia.setPermiteRepetir(false);
        consistencia.setOpciones(new ArrayList<>());
        consistencia.getOpciones().add(opcion(consistencia, 9, "Cremoso", 0));
        consistencia.getOpciones().add(opcion(consistencia, 10, "Líquido", 1));

        producto.getGruposOpciones().add(sabores);
        producto.getGruposOpciones().add(consistencia);
        return producto;
    }

    private static ProductoOpcion opcion(ProductoGrupoOpcion grupo, int id, String nombre, int orden) {
        ProductoOpcion o = new ProductoOpcion();
        o.setId(id);
        o.setGrupo(grupo);
        o.setNombre(nombre);
        o.setOrden(orden);
        o.setActivo(true);
        return o;
    }

    private static PedidoItemOpcionResponseDTO sel(int grupoId, int opcionId, int cantidad) {
        PedidoItemOpcionResponseDTO dto = new PedidoItemOpcionResponseDTO();
        dto.setGrupoId(grupoId);
        dto.setOpcionId(opcionId);
        dto.setCantidad(cantidad);
        return dto;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
