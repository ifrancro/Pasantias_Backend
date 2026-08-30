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
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.ProductoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductoGruposOpcionesSyncTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ClubRepository clubRepository;
    @Mock
    private HubRepository hubRepository;
    @Mock
    private ClubProductoRepository clubProductoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private Usuario host;
    private Producto producto;

    @BeforeEach
    void setUp() {
        host = anfitrion(20);
        producto = productoSaboresYConsistencia(host);
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void editarNombreGrupoConservaGroupId() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "Sabores disponibles", 1, 1, 3, true,
                        opcionDto(6, "Frutilla", 0),
                        opcionDto(7, "Cookies", 1),
                        opcionDto(8, "Durazno", 2)),
                grupoDto(4, "Consistencia", 1, 1, 1, false,
                        opcionDto(9, "Cremoso", 0),
                        opcionDto(10, "Líquido", 1))));

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals(3, dto.getGruposOpciones().get(0).getId());
        assertEquals("Sabores disponibles", dto.getGruposOpciones().get(0).getNombre());
    }

    @Test
    void editarOrdenYReglasConservaGroupId() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "Sabores", 2, 0, 2, false,
                        opcionDto(6, "Frutilla", 0),
                        opcionDto(7, "Cookies", 1),
                        opcionDto(8, "Durazno", 2)),
                grupoDto(4, "Consistencia", 0, 2, 2, true,
                        opcionDto(9, "Cremoso", 0),
                        opcionDto(10, "Líquido", 1))));

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals(4, dto.getGruposOpciones().stream()
                .filter(g -> "Consistencia".equals(g.getNombre())).findFirst().orElseThrow().getId());
        assertEquals(0, dto.getGruposOpciones().stream()
                .filter(g -> "Consistencia".equals(g.getNombre())).findFirst().orElseThrow().getOrden());
        assertEquals(2, dto.getGruposOpciones().stream()
                .filter(g -> "Consistencia".equals(g.getNombre())).findFirst().orElseThrow().getMinSelecciones());
        assertTrue(dto.getGruposOpciones().stream()
                .filter(g -> "Consistencia".equals(g.getNombre())).findFirst().orElseThrow().getPermiteRepetir());
    }

    @Test
    void agregarGrupoGeneraSoloUnGrupoNuevo() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "Sabores", 0, 1, 3, true,
                        opcionDto(6, "Frutilla", 0),
                        opcionDto(7, "Cookies", 1),
                        opcionDto(8, "Durazno", 2)),
                grupoDto(4, "Consistencia", 1, 1, 1, false,
                        opcionDto(9, "Cremoso", 0),
                        opcionDto(10, "Líquido", 1)),
                grupoDto(null, "Toppings", 2, 0, 1, false, opcionDto(null, "Granola", 0))));

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals(3, dto.getGruposOpciones().size());
        assertNull(dto.getGruposOpciones().stream()
                .filter(g -> "Toppings".equals(g.getNombre())).findFirst().orElseThrow().getId());
        assertEquals(List.of(3, 4),
                dto.getGruposOpciones().stream()
                        .filter(g -> !"Toppings".equals(g.getNombre()))
                        .map(ProductoGrupoOpcionDTO::getId).sorted().toList());
    }

    @Test
    void eliminarGrupoNoCambiaIdsDeRestantes() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "Sabores", 0, 1, 3, true,
                        opcionDto(6, "Frutilla", 0),
                        opcionDto(7, "Cookies", 1),
                        opcionDto(8, "Durazno", 2))));

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals(1, dto.getGruposOpciones().size());
        assertEquals(3, dto.getGruposOpciones().get(0).getId());
        assertEquals(List.of(6, 7, 8),
                dto.getGruposOpciones().get(0).getOpciones().stream().map(ProductoOpcionDTO::getId).toList());
    }

    @Test
    void groupIdDeOtroProductoRechaza400() {
        Producto otro = productoSaboresYConsistencia(host);
        ProductoGrupoOpcion ajeno = otro.getGruposOpciones().get(0);
        ajeno.setId(999);

        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(999, "Hack", 0, 1, 1, false, opcionDto(null, "X", 0))));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.updateProducto(10, request, 20));
        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    void groupIdRepetidoRechaza400() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "A", 0, 1, 1, false, opcionDto(6, "X", 0)),
                grupoDto(3, "B", 1, 1, 1, false, opcionDto(7, "Y", 0))));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.updateProducto(10, request, 20));
        assertTrue(ex.getMessage().toLowerCase().contains("repetido"));
    }

    @Test
    void editarNombreOpcionConservaOptionId() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "Sabores", 0, 1, 3, true,
                        opcionDto(6, "Frutilla", 0),
                        opcionDto(7, "Cookies", 1),
                        opcionDto(8, "Mango", 2)),
                grupoDto(4, "Consistencia", 1, 1, 1, false,
                        opcionDto(9, "Cremoso", 0),
                        opcionDto(10, "Líquido", 1))));

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals(8, dto.getGruposOpciones().get(0).getOpciones().stream()
                .filter(o -> "Mango".equals(o.getNombre())).findFirst().orElseThrow().getId());
    }

    @Test
    void agregarOpcionGeneraSoloUnIdNuevo() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "Sabores", 0, 1, 3, true,
                        opcionDto(6, "Frutilla", 0),
                        opcionDto(7, "Cookies", 1),
                        opcionDto(8, "Durazno", 2),
                        opcionDto(null, "Vainilla", 3)),
                grupoDto(4, "Consistencia", 1, 1, 1, false,
                        opcionDto(9, "Cremoso", 0),
                        opcionDto(10, "Líquido", 1))));

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        List<Integer> ids = dto.getGruposOpciones().get(0).getOpciones().stream()
                .map(ProductoOpcionDTO::getId).toList();
        assertEquals(List.of(6, 7, 8), ids.stream().filter(id -> id != null).sorted().toList());
        assertEquals(1, ids.stream().filter(id -> id == null).count());
    }

    @Test
    void eliminarOpcionNoCambiaIdsRestantes() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "Sabores", 0, 1, 2, true,
                        opcionDto(6, "Frutilla", 0),
                        opcionDto(8, "Durazno", 1)),
                grupoDto(4, "Consistencia", 1, 1, 1, false,
                        opcionDto(9, "Cremoso", 0),
                        opcionDto(10, "Líquido", 1))));

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals(List.of(6, 8),
                dto.getGruposOpciones().get(0).getOpciones().stream().map(ProductoOpcionDTO::getId).toList());
    }

    @Test
    void optionIdDeOtroGrupoRechaza400() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "Sabores", 0, 1, 2, true,
                        opcionDto(9, "Cremoso", 0),
                        opcionDto(6, "Frutilla", 1)),
                grupoDto(4, "Consistencia", 1, 1, 1, false,
                        opcionDto(10, "Líquido", 0))));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.updateProducto(10, request, 20));
        assertTrue(ex.getMessage().contains("9"));
    }

    @Test
    void optionIdRepetidoRechaza400() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "Sabores", 0, 1, 2, true,
                        opcionDto(6, "Frutilla", 0),
                        opcionDto(6, "Cookies", 1))));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.updateProducto(10, request, 20));
        assertTrue(ex.getMessage().toLowerCase().contains("repetido"));
    }

    @Test
    void moverOptionIdEntreGruposRechaza400() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "Sabores", 0, 1, 1, true, opcionDto(6, "Frutilla", 0)),
                grupoDto(4, "Consistencia", 1, 1, 1, false,
                        opcionDto(7, "Cookies", 0),
                        opcionDto(10, "Líquido", 1))));

        assertThrows(IllegalArgumentException.class,
                () -> productoService.updateProducto(10, request, 20));
    }

    @Test
    void existentePreservaActivoIgnorandoDto() {
        ProductoOpcion cookies = producto.getGruposOpciones().get(0).getOpciones().stream()
                .filter(o -> o.getId() == 7).findFirst().orElseThrow();
        cookies.setActivo(false);

        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(payloadCompleto());

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        ProductoOpcionDTO cookiesDto = dto.getGruposOpciones().get(0).getOpciones().stream()
                .filter(o -> Integer.valueOf(7).equals(o.getId())).findFirst().orElseThrow();
        assertFalse(cookiesDto.getActivo());
    }

    @Test
    void nuevaOpcionNaceActiva() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "Sabores", 0, 1, 3, true,
                        opcionDto(6, "Frutilla", 0),
                        opcionDto(7, "Cookies", 1),
                        opcionDto(8, "Durazno", 2),
                        opcionDto(null, "Vainilla", 3))));

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        ProductoOpcionDTO nueva = dto.getGruposOpciones().get(0).getOpciones().stream()
                .filter(o -> "Vainilla".equals(o.getNombre())).findFirst().orElseThrow();
        assertTrue(nueva.getActivo());
    }

    @Test
    void noOpConservaTodosLosIdsYAprobado() {
        producto.setEstadoAprobacion("APROBADO");
        producto.setRevisadoPor(admin(7));
        producto.setRevisadoAt(LocalDateTime.of(2026, 8, 1, 10, 0));

        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(payloadCompleto());

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals("APROBADO", dto.getEstadoAprobacion());
        assertEquals(3, dto.getGruposOpciones().get(0).getId());
        assertEquals(List.of(6, 7, 8),
                dto.getGruposOpciones().get(0).getOpciones().stream().map(ProductoOpcionDTO::getId).toList());
    }

    @Test
    void cambioEstructuralConservaIdsPeroPasaAPendiente() {
        producto.setEstadoAprobacion("APROBADO");
        producto.setRevisadoPor(admin(7));
        producto.setRevisadoAt(LocalDateTime.of(2026, 8, 1, 10, 0));

        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "Sabores", 0, 1, 3, true,
                        opcionDto(6, "Frutilla", 0),
                        opcionDto(7, "Cookies", 1),
                        opcionDto(8, "Mango", 2)),
                grupoDto(4, "Consistencia", 1, 1, 1, false,
                        opcionDto(9, "Cremoso", 0),
                        opcionDto(10, "Líquido", 1))));

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals("PENDIENTE", dto.getEstadoAprobacion());
        assertEquals(8, dto.getGruposOpciones().get(0).getOpciones().stream()
                .filter(o -> "Mango".equals(o.getNombre())).findFirst().orElseThrow().getId());
    }

    @Test
    void rechazadoPermaneceRechazado() {
        producto.setEstadoAprobacion("RECHAZADO");
        producto.setComentarioRevision("No");

        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(payloadCompleto());

        ProductoDTO dto = productoService.updateProducto(10, request, 20);
        assertEquals("RECHAZADO", dto.getEstadoAprobacion());
    }

    @Test
    void legacySinIdsReutilizaPorNombre() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(null, "Sabores", 0, 1, 3, true,
                        opcionDto(null, "Frutilla", 0),
                        opcionDto(null, "Cookies", 1),
                        opcionDto(null, "Durazno", 2)),
                grupoDto(null, "Consistencia", 1, 1, 1, false,
                        opcionDto(null, "Cremoso", 0),
                        opcionDto(null, "Líquido", 1))));

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals(3, dto.getGruposOpciones().get(0).getId());
        assertEquals(List.of(6, 7, 8),
                dto.getGruposOpciones().get(0).getOpciones().stream().map(ProductoOpcionDTO::getId).toList());
    }

    @Test
    void swapNombresOpcionesSinViolacion() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "Sabores", 0, 1, 2, true,
                        opcionDto(6, "Cookies", 0),
                        opcionDto(7, "Frutilla", 1),
                        opcionDto(8, "Durazno", 2)),
                grupoDto(4, "Consistencia", 1, 1, 1, false,
                        opcionDto(9, "Cremoso", 0),
                        opcionDto(10, "Líquido", 1))));

        ProductoDTO dto = assertDoesNotThrow(
                () -> productoService.updateProducto(10, request, 20));

        assertEquals(6, dto.getGruposOpciones().get(0).getOpciones().stream()
                .filter(o -> "Cookies".equals(o.getNombre())).findFirst().orElseThrow().getId());
        assertEquals(7, dto.getGruposOpciones().get(0).getOpciones().stream()
                .filter(o -> "Frutilla".equals(o.getNombre())).findFirst().orElseThrow().getId());
    }

    @Test
    void swapNombresGruposSinViolacion() {
        ProductoDTO request = baseUpdate();
        request.setGruposOpciones(List.of(
                grupoDto(3, "Consistencia", 0, 1, 1, false,
                        opcionDto(6, "Frutilla", 0),
                        opcionDto(7, "Cookies", 1),
                        opcionDto(8, "Durazno", 2)),
                grupoDto(4, "Sabores", 1, 1, 1, false,
                        opcionDto(9, "Cremoso", 0),
                        opcionDto(10, "Líquido", 1))));

        ProductoDTO dto = assertDoesNotThrow(
                () -> productoService.updateProducto(10, request, 20));

        assertEquals(3, dto.getGruposOpciones().stream()
                .filter(g -> "Consistencia".equals(g.getNombre())).findFirst().orElseThrow().getId());
        assertEquals(4, dto.getGruposOpciones().stream()
                .filter(g -> "Sabores".equals(g.getNombre())).findFirst().orElseThrow().getId());
    }

    private List<ProductoGrupoOpcionDTO> payloadCompleto() {
        return List.of(
                grupoDto(3, "Sabores", 0, 1, 3, true,
                        opcionDto(6, "Frutilla", 0),
                        opcionDto(7, "Cookies", 1),
                        opcionDto(8, "Durazno", 2)),
                grupoDto(4, "Consistencia", 1, 1, 1, false,
                        opcionDto(9, "Cremoso", 0),
                        opcionDto(10, "Líquido", 1)));
    }

    private ProductoDTO baseUpdate() {
        ProductoDTO request = new ProductoDTO();
        request.setNombre(producto.getNombre());
        request.setDescripcion(producto.getDescripcion());
        request.setIngredientes(producto.getIngredientes());
        request.setPuntosValor(producto.getPuntosValor());
        request.setPrecio(producto.getPrecio());
        return request;
    }

    private static ProductoGrupoOpcionDTO grupoDto(
            Integer id, String nombre, int orden, int min, Integer max, boolean repetir,
            ProductoOpcionDTO... opciones) {
        ProductoGrupoOpcionDTO dto = new ProductoGrupoOpcionDTO();
        dto.setId(id);
        dto.setNombre(nombre);
        dto.setOrden(orden);
        dto.setMinSelecciones(min);
        dto.setMaxSelecciones(max);
        dto.setPermiteRepetir(repetir);
        dto.setOpciones(new ArrayList<>(List.of(opciones)));
        return dto;
    }

    private static ProductoOpcionDTO opcionDto(Integer id, String nombre, int orden) {
        ProductoOpcionDTO dto = new ProductoOpcionDTO();
        dto.setId(id);
        dto.setNombre(nombre);
        dto.setOrden(orden);
        dto.setActivo(false);
        return dto;
    }

    private static Producto productoSaboresYConsistencia(Usuario host) {
        Producto producto = new Producto();
        producto.setId(10);
        producto.setNombre("Batido");
        producto.setDescripcion("Proteico");
        producto.setIngredientes("proteína");
        producto.setPuntosValor(10);
        producto.setPrecio(BigDecimal.ZERO);
        producto.setHub(hub());
        producto.setClubCreador(clubDelAnfitrion(host));
        producto.setTipo("LOCAL");
        producto.setEstadoAprobacion("PENDIENTE");
        producto.setActivo(true);
        producto.setGruposOpciones(new ArrayList<>());

        ProductoGrupoOpcion sabores = grupoEntity(producto, 3, "Sabores", 0);
        sabores.setMinSelecciones(1);
        sabores.setMaxSelecciones(3);
        sabores.setPermiteRepetir(true);
        sabores.getOpciones().add(opcionEntity(sabores, 6, "Frutilla", 0));
        sabores.getOpciones().add(opcionEntity(sabores, 7, "Cookies", 1));
        sabores.getOpciones().add(opcionEntity(sabores, 8, "Durazno", 2));

        ProductoGrupoOpcion consistencia = grupoEntity(producto, 4, "Consistencia", 1);
        consistencia.setMinSelecciones(1);
        consistencia.setMaxSelecciones(1);
        consistencia.setPermiteRepetir(false);
        consistencia.getOpciones().add(opcionEntity(consistencia, 9, "Cremoso", 0));
        consistencia.getOpciones().add(opcionEntity(consistencia, 10, "Líquido", 1));

        producto.getGruposOpciones().add(sabores);
        producto.getGruposOpciones().add(consistencia);
        return producto;
    }

    private static ProductoGrupoOpcion grupoEntity(Producto producto, int id, String nombre, int orden) {
        ProductoGrupoOpcion grupo = new ProductoGrupoOpcion();
        grupo.setId(id);
        grupo.setProducto(producto);
        grupo.setNombre(nombre);
        grupo.setOrden(orden);
        grupo.setMinSelecciones(0);
        grupo.setPermiteRepetir(false);
        grupo.setOpciones(new ArrayList<>());
        return grupo;
    }

    private static ProductoOpcion opcionEntity(ProductoGrupoOpcion grupo, int id, String nombre, int orden) {
        ProductoOpcion opcion = new ProductoOpcion();
        opcion.setId(id);
        opcion.setGrupo(grupo);
        opcion.setNombre(nombre);
        opcion.setOrden(orden);
        opcion.setActivo(true);
        return opcion;
    }

    private static Usuario admin(int id) {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        return usuario;
    }

    private static Usuario anfitrion(int id) {
        Rol rol = new Rol();
        rol.setNombre("ANFITRION");
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        return usuario;
    }

    private static Hub hub() {
        Hub hub = new Hub();
        hub.setId(1);
        return hub;
    }

    private static Club clubDelAnfitrion(Usuario host) {
        Club club = new Club();
        club.setId(5);
        club.setAnfitrion(host);
        club.setHub(hub());
        return club;
    }
}
