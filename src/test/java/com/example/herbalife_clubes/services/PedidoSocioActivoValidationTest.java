package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.Producto;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoSocioActivoValidationTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private MembresiaRepository membresiaRepository;
    @Mock
    private ClubRepository clubRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ClubProductoRepository clubProductoRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ComboRepository comboRepository;

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    @Test
    void pedidoSocioConActivoFalseSeRechaza() {
        stubSocioClubProducto(false, true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.createPedido(new PedidoDTO(), 1, 3, 10));
        assertTrue(ex.getMessage().toLowerCase().contains("activo"));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void pedidoSocioValidoSeMantiene() {
        stubSocioClubProducto(true, true);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido pedido = inv.getArgument(0);
            pedido.setId(50);
            return pedido;
        });

        PedidoDTO dto = pedidoService.createPedido(new PedidoDTO(), 1, 3, 10);

        assertEquals(50, dto.getId());
        verify(pedidoRepository).save(any(Pedido.class));
    }

    private void stubSocioClubProducto(boolean activo, boolean disponible) {
        Usuario host = new Usuario();
        host.setId(20);
        Hub hub = new Hub();
        hub.setId(1);
        Club club = new Club();
        club.setId(3);
        club.setEstado("ACTIVO");
        club.setNombreClub("Club 3");
        club.setHub(hub);
        club.setAnfitrion(host);

        Membresia membresia = new Membresia();
        membresia.setId(1);
        membresia.setEstado("ACTIVA");
        membresia.setNumeroSocio("SC-1");
        membresia.setClub(club);

        Producto producto = new Producto();
        producto.setId(10);
        producto.setNombre("Batido");
        producto.setEstadoAprobacion("APROBADO");
        producto.setActivo(activo);
        producto.setPrecio(BigDecimal.ZERO);
        producto.setHub(hub);

        ClubProducto cp = new ClubProducto();
        cp.setClub(club);
        cp.setProducto(producto);
        cp.setDisponible(disponible);

        when(membresiaRepository.findById(1)).thenReturn(Optional.of(membresia));
        when(clubRepository.findById(3)).thenReturn(Optional.of(club));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        if (activo) {
            when(clubProductoRepository.findByClubIdAndProductoId(3, 10)).thenReturn(Optional.of(cp));
        }
    }
}
