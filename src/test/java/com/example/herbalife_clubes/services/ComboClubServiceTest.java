package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.combo.ComboCreateRequest;
import com.example.herbalife_clubes.dtos.combo.ComboDTO;
import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.pedidos.PedidoComboSupport;
import com.example.herbalife_clubes.repositories.*;
import com.example.herbalife_clubes.serviceimpls.ComboClubServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComboClubServiceTest {

    @Mock
    private ComboRepository comboRepository;
    @Mock
    private ClubRepository clubRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private SaborRepository saborRepository;
    @Mock
    private ClubProductoRepository clubProductoRepository;

    @InjectMocks
    private ComboClubServiceImpl service;

    private Club club;
    private Hub hub;

    @BeforeEach
    void setUp() {
        hub = new Hub();
        hub.setId(1);
        club = new Club();
        club.setId(3);
        club.setHub(hub);
        club.setNombreClub("Club 3");
    }

    @Test
    void createComboExigePrecioMayorCero() {
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));

        ComboCreateRequest request = requestBase();
        request.setPrecio(BigDecimal.ZERO);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createCombo(3, request));
        assertEquals(PedidoComboSupport.MENSAJE_PRECIO_COMBO_INVALIDO, ex.getMessage());
    }

    @Test
    void createComboRecalculaPuntosIgnorandoRequest() {
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        Producto p1 = globalProducto(7, 10);
        Producto p2 = globalProducto(2, 5);
        when(productoRepository.findById(7)).thenReturn(Optional.of(p1));
        when(productoRepository.findById(2)).thenReturn(Optional.of(p2));
        when(comboRepository.save(any(Combo.class))).thenAnswer(inv -> {
            Combo c = inv.getArgument(0);
            c.setId(4);
            return c;
        });
        when(clubProductoRepository.findByClubIdAndProductoId(3, 7)).thenReturn(Optional.of(disponible()));
        when(clubProductoRepository.findByClubIdAndProductoId(3, 2)).thenReturn(Optional.of(disponible()));

        ComboCreateRequest request = requestBase();
        request.setPrecio(bd("38"));
        request.setPuntosValor(999);

        ComboDTO dto = service.createCombo(3, request);

        assertEquals(15, dto.getPuntosValor());
        assertEquals(0, bd("38").compareTo(dto.getPrecio()));
        assertTrue(dto.getDisponible());

        ArgumentCaptor<Combo> captor = ArgumentCaptor.forClass(Combo.class);
        verify(comboRepository).save(captor.capture());
        assertEquals(15, captor.getValue().getPuntosValor());
    }

    @Test
    void createComboRechazaProductoLocalDeOtroClub() {
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        Producto ajeno = new Producto();
        ajeno.setId(50);
        ajeno.setNombre("Local ajeno");
        ajeno.setTipo("LOCAL");
        ajeno.setEstadoAprobacion("APROBADO");
        ajeno.setActivo(true);
        Club otro = new Club();
        otro.setId(99);
        ajeno.setClubCreador(otro);
        when(productoRepository.findById(50)).thenReturn(Optional.of(ajeno));

        ComboCreateRequest request = requestBase();
        request.setPrecio(bd("38"));
        request.getItems().get(0).setProductoId(50);

        assertThrows(IllegalArgumentException.class, () -> service.createCombo(3, request));
    }

    @Test
    void createComboRechazaGlobalDeOtroHub() {
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        Producto ajeno = globalProducto(60, 5);
        Hub otroHub = new Hub();
        otroHub.setId(99);
        ajeno.setHub(otroHub);
        when(productoRepository.findById(60)).thenReturn(Optional.of(ajeno));

        ComboCreateRequest request = requestBase();
        request.setPrecio(bd("38"));
        request.getItems().get(0).setProductoId(60);

        assertThrows(IllegalArgumentException.class, () -> service.createCombo(3, request));
    }

    private static ComboCreateRequest requestBase() {
        ComboCreateRequest request = new ComboCreateRequest();
        request.setNombre("Combo test");
        ComboCreateRequest.ComboItemRequest i1 = new ComboCreateRequest.ComboItemRequest();
        i1.setProductoId(7);
        i1.setCantidad(1);
        ComboCreateRequest.ComboItemRequest i2 = new ComboCreateRequest.ComboItemRequest();
        i2.setProductoId(2);
        i2.setCantidad(1);
        request.setItems(List.of(i1, i2));
        return request;
    }

    private Producto globalProducto(int id, int puntos) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre("P" + id);
        p.setTipo("GLOBAL");
        p.setHub(hub);
        p.setEstadoAprobacion("APROBADO");
        p.setActivo(true);
        p.setPuntosValor(puntos);
        return p;
    }

    private static ClubProducto disponible() {
        ClubProducto cp = new ClubProducto();
        cp.setDisponible(true);
        return cp;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
