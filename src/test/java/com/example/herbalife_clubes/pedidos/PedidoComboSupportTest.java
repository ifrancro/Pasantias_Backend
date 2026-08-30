package com.example.herbalife_clubes.pedidos;

import com.example.herbalife_clubes.dtos.pedido.PedidoComboComponenteRequestDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoComboRequestDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemOpcionResponseDTO;
import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.ComboRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PedidoComboSupportTest {

    @Mock
    private ComboRepository comboRepository;
    @Mock
    private ClubProductoRepository clubProductoRepository;

    @InjectMocks
    private PedidoComboSupport support;

    private Club club;
    private Combo combo;

    @BeforeEach
    void setUp() {
        club = new Club();
        club.setId(3);

        Producto batido = producto(7, "Batido", 20);
        Producto te = producto(2, "Té", 15);

        combo = new Combo();
        combo.setId(4);
        combo.setClub(club);
        combo.setNombre("Combo desayuno");
        combo.setActivo(true);
        combo.setPrecio(bd("38.00"));
        combo.setPuntosValor(15);

        ComboItem ci1 = comboItem(combo, batido, 1);
        ComboItem ci2 = comboItem(combo, te, 1);
        combo.setItems(new ArrayList<>(List.of(ci1, ci2)));

        when(comboRepository.findByIdWithItems(4)).thenReturn(Optional.of(combo));
        when(clubProductoRepository.findByClubIdAndProductoId(anyInt(), anyInt()))
                .thenAnswer(inv -> {
                    ClubProducto cp = new ClubProducto();
                    cp.setDisponible(true);
                    return Optional.of(cp);
                });
    }

    @Test
    void materializaComboConPrecioCongeladoYComponentesCero() {
        Pedido pedido = new Pedido();
        pedido.setClub(club);

        PedidoComboRequestDTO request = new PedidoComboRequestDTO();
        request.setComboId(4);
        request.setCantidad(2);
        request.setComponentes(List.of(
                componente(7, List.of()),
                componente(2, List.of())));

        PedidoCombo pedidoCombo = support.materializar(pedido, request, 3);

        assertEquals("Combo desayuno", pedidoCombo.getComboNombreSnapshot());
        assertEquals(0, bd("38.00").compareTo(pedidoCombo.getPrecioUnitarioSnapshot()));
        assertEquals(0, bd("76.00").compareTo(pedidoCombo.getSubtotalSnapshot()));
        assertEquals(15, pedidoCombo.getPuntosValorSnapshot());
        assertEquals(2, pedidoCombo.getItems().size());
        assertEquals(2, pedido.getItems().size());

        for (PedidoItem item : pedidoCombo.getItems()) {
            assertSame(pedidoCombo, item.getPedidoCombo());
            assertEquals(0, BigDecimal.ZERO.compareTo(item.getSubtotal()));
            assertEquals(0, BigDecimal.ZERO.compareTo(item.getPrecioUnitario()));
        }
        assertEquals(2, pedidoCombo.getItems().stream().filter(i -> i.getProducto().getId() == 7).findFirst().orElseThrow().getCantidad());
    }

    @Test
    void comboItemCantidadMayorMultiplicaCantidadEfectiva() {
        combo.getItems().get(0).setCantidad(2);
        Pedido pedido = new Pedido();

        PedidoComboRequestDTO request = new PedidoComboRequestDTO();
        request.setComboId(4);
        request.setCantidad(3);
        request.setComponentes(List.of(componente(7, List.of()), componente(2, List.of())));

        PedidoCombo pedidoCombo = support.materializar(pedido, request, 3);

        PedidoItem batido = pedidoCombo.getItems().stream()
                .filter(i -> i.getProducto().getId() == 7)
                .findFirst()
                .orElseThrow();
        assertEquals(6, batido.getCantidad());
    }

    @Test
    void componenteExtraRechazado() {
        Pedido pedido = new Pedido();
        PedidoComboRequestDTO request = new PedidoComboRequestDTO();
        request.setComboId(4);
        request.setCantidad(1);
        request.setComponentes(List.of(
                componente(7, List.of()),
                componente(2, List.of()),
                componente(99, List.of())));

        assertThrows(IllegalArgumentException.class, () -> support.materializar(pedido, request, 3));
    }

    @Test
    void componenteFaltanteRechazado() {
        Pedido pedido = new Pedido();
        PedidoComboRequestDTO request = new PedidoComboRequestDTO();
        request.setComboId(4);
        request.setCantidad(1);
        request.setComponentes(List.of(componente(7, List.of())));

        assertThrows(IllegalArgumentException.class, () -> support.materializar(pedido, request, 3));
    }

    @Test
    void comboOtroClubRechazado() {
        club.setId(99);
        Pedido pedido = new Pedido();
        PedidoComboRequestDTO request = new PedidoComboRequestDTO();
        request.setComboId(4);
        request.setCantidad(1);
        request.setComponentes(List.of(componente(7, List.of()), componente(2, List.of())));

        assertThrows(IllegalArgumentException.class, () -> support.materializar(pedido, request, 3));
    }

    @Test
    void comboInactivoRechazado() {
        combo.setActivo(false);
        Pedido pedido = new Pedido();
        PedidoComboRequestDTO request = new PedidoComboRequestDTO();
        request.setComboId(4);
        request.setCantidad(1);
        request.setComponentes(List.of(componente(7, List.of()), componente(2, List.of())));

        assertThrows(IllegalArgumentException.class, () -> support.materializar(pedido, request, 3));
    }

    @Test
    void comboPrecioCeroRechazado() {
        combo.setPrecio(BigDecimal.ZERO);
        Pedido pedido = new Pedido();
        PedidoComboRequestDTO request = new PedidoComboRequestDTO();
        request.setComboId(4);
        request.setCantidad(1);
        request.setComponentes(List.of(componente(7, List.of()), componente(2, List.of())));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> support.materializar(pedido, request, 3));
        assertEquals(PedidoComboSupport.MENSAJE_PRECIO_COMBO_INVALIDO, ex.getMessage());
    }

    @Test
    void pedidoMixtoPermiteComboMasProductoSuelto() {
        assertDoesNotThrow(() -> PedidoComboSupport.validarComposicion(combo, List.of(
                componente(7, List.of()),
                componente(2, List.of()))));
    }

    private static PedidoComboComponenteRequestDTO componente(int productoId, List<PedidoItemOpcionResponseDTO> opciones) {
        PedidoComboComponenteRequestDTO dto = new PedidoComboComponenteRequestDTO();
        dto.setProductoId(productoId);
        dto.setOpciones(opciones);
        return dto;
    }

    private static ComboItem comboItem(Combo combo, Producto producto, int cantidad) {
        ComboItem item = new ComboItem();
        item.setCombo(combo);
        item.setProducto(producto);
        item.setCantidad(cantidad);
        return item;
    }

    private static Producto producto(int id, String nombre, int puntos) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre(nombre);
        p.setPuntosValor(puntos);
        p.setEstadoAprobacion("APROBADO");
        p.setActivo(true);
        p.setTipo("GLOBAL");
        return p;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
