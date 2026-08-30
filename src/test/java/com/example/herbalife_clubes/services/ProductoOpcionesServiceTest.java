package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoGrupoOpcionDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoOpcionDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.ClubProducto;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

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
class ProductoOpcionesServiceTest {

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

    @Test
    void anfitrionCreaLocalConDosGruposPendiente() {
        stubCreateAnfitrion();
        ProductoDTO request = baseCreate();
        request.setGruposOpciones(List.of(
                grupo("Sabores", 0, 1, 2, true,
                        opcion("Frutilla", 0), opcion("Vainilla", 1)),
                grupo("Consistencia", 1, 1, 1, false,
                        opcion("Cremoso", 0), opcion("Líquido", 1))));

        ProductoDTO dto = productoService.createProducto(request, 20, 1);

        assertEquals("LOCAL", dto.getTipo());
        assertEquals("PENDIENTE", dto.getEstadoAprobacion());
        assertEquals(2, dto.getGruposOpciones().size());
        ProductoGrupoOpcionDTO sabores = dto.getGruposOpciones().get(0);
        assertEquals("Sabores", sabores.getNombre());
        assertEquals(1, sabores.getMinSelecciones());
        assertEquals(2, sabores.getMaxSelecciones());
        assertTrue(sabores.getPermiteRepetir());
        assertEquals(List.of("Frutilla", "Vainilla"),
                sabores.getOpciones().stream().map(ProductoOpcionDTO::getNombre).toList());
        assertTrue(sabores.getOpciones().stream().allMatch(ProductoOpcionDTO::getActivo));
        ProductoGrupoOpcionDTO cons = dto.getGruposOpciones().get(1);
        assertEquals("Consistencia", cons.getNombre());
        assertFalse(cons.getPermiteRepetir());
        assertEquals(1, cons.getMaxSelecciones());
    }

    @Test
    void createSinGruposSigueFuncionando() {
        stubCreateAnfitrion();
        ProductoDTO dto = productoService.createProducto(baseCreate(), 20, 1);
        assertEquals("PENDIENTE", dto.getEstadoAprobacion());
        assertNotNull(dto.getGruposOpciones());
        assertTrue(dto.getGruposOpciones().isEmpty());
    }

    @Test
    void clienteViejoSinGruposOpcionesNoRompe() {
        stubCreateAnfitrion();
        ProductoDTO request = baseCreate();
        request.setGruposOpciones(null);
        ProductoDTO dto = productoService.createProducto(request, 20, 1);
        assertEquals("LOCAL", dto.getTipo());
        assertTrue(dto.getGruposOpciones().isEmpty());
    }

    @Test
    void nombreGrupoVacioLanza400() {
        stubCreateAnfitrion();
        ProductoDTO request = baseCreate();
        request.setGruposOpciones(List.of(grupo("  ", 0, 0, null, false, opcion("A", 0))));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.createProducto(request, 20, 1));
        assertTrue(ex.getMessage().toLowerCase().contains("nombre"));
    }

    @Test
    void grupoDuplicadoCaseInsensitiveLanza400() {
        stubCreateAnfitrion();
        ProductoDTO request = baseCreate();
        request.setGruposOpciones(List.of(
                grupo("Sabores", 0, 0, null, false, opcion("A", 0)),
                grupo("sabores", 1, 0, null, false, opcion("B", 0))));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.createProducto(request, 20, 1));
        assertTrue(ex.getMessage().toLowerCase().contains("existe"));
    }

    @Test
    void opcionVaciaLanza400() {
        stubCreateAnfitrion();
        ProductoDTO request = baseCreate();
        request.setGruposOpciones(List.of(grupo("Sabores", 0, 0, null, false, opcion(" ", 0))));
        assertThrows(IllegalArgumentException.class, () -> productoService.createProducto(request, 20, 1));
    }

    @Test
    void opcionDuplicadaLanza400() {
        stubCreateAnfitrion();
        ProductoDTO request = baseCreate();
        request.setGruposOpciones(List.of(
                grupo("Sabores", 0, 0, null, false, opcion("Frutilla", 0), opcion("frutilla", 1))));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.createProducto(request, 20, 1));
        assertTrue(ex.getMessage().toLowerCase().contains("duplicada"));
    }

    @Test
    void minNegativoLanza400() {
        stubCreateAnfitrion();
        ProductoDTO request = baseCreate();
        request.setGruposOpciones(List.of(grupo("Sabores", 0, -1, null, false, opcion("A", 0))));
        assertThrows(IllegalArgumentException.class, () -> productoService.createProducto(request, 20, 1));
    }

    @Test
    void maxMenorQueMinLanza400() {
        stubCreateAnfitrion();
        ProductoDTO request = baseCreate();
        request.setGruposOpciones(List.of(grupo("Sabores", 0, 2, 1, false, opcion("A", 0), opcion("B", 1))));
        assertThrows(IllegalArgumentException.class, () -> productoService.createProducto(request, 20, 1));
    }

    @Test
    void maxImposibleSinRepeticionLanza400() {
        stubCreateAnfitrion();
        ProductoDTO request = baseCreate();
        request.setGruposOpciones(List.of(
                grupo("Sabores", 0, 1, 3, false, opcion("A", 0), opcion("B", 1))));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.createProducto(request, 20, 1));
        assertTrue(ex.getMessage().toLowerCase().contains("repet"));
    }

    @Test
    void grupoSinOpcionesLanza400() {
        stubCreateAnfitrion();
        ProductoDTO request = baseCreate();
        ProductoGrupoOpcionDTO vacio = grupo("Sabores", 0, 0, null, false);
        vacio.setOpciones(List.of());
        request.setGruposOpciones(List.of(vacio));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.createProducto(request, 20, 1));
        assertTrue(ex.getMessage().toLowerCase().contains("opción")
                || ex.getMessage().toLowerCase().contains("opcion"));
    }

    @Test
    void putGruposNullPreservaExistentes() {
        Usuario host = anfitrion(20);
        Producto producto = productoConGrupos(host);
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO request = baseUpdate(producto);
        request.setGruposOpciones(null);

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals(1, producto.getGruposOpciones().size());
        assertEquals("Sabores", producto.getGruposOpciones().get(0).getNombre());
        assertEquals(1, dto.getGruposOpciones().size());
        assertEquals("RECHAZADO", dto.getEstadoAprobacion());
    }

    @Test
    void putGruposVacioEliminaTodos() {
        Usuario host = anfitrion(20);
        Producto producto = productoConGrupos(host);
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO request = baseUpdate(producto);
        request.setGruposOpciones(List.of());

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertTrue(producto.getGruposOpciones().isEmpty());
        assertTrue(dto.getGruposOpciones().isEmpty());
    }

    @Test
    void putReemplazoCompletoFunciona() {
        Usuario host = anfitrion(20);
        Producto producto = productoConGrupos(host);
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO request = baseUpdate(producto);
        request.setGruposOpciones(List.of(
                grupo("Temperatura", 0, 1, 1, false, opcion("Caliente", 0), opcion("Frío", 1))));

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals(1, dto.getGruposOpciones().size());
        assertEquals("Temperatura", dto.getGruposOpciones().get(0).getNombre());
        assertEquals(2, dto.getGruposOpciones().get(0).getOpciones().size());
    }

    @Test
    void anfitrionNoPuedeModificarLocalAjeno() {
        Usuario otro = anfitrion(99);
        when(usuarioRepository.findById(99)).thenReturn(Optional.of(otro));
        when(productoRepository.findById(10)).thenReturn(Optional.of(productoConGrupos(anfitrion(20))));

        ProductoDTO request = new ProductoDTO();
        request.setNombre("Hack");
        assertThrows(AccessDeniedException.class, () -> productoService.updateProducto(10, request, 99));
    }

    @Test
    void anfitrionAprobadoCambioEstructuralPasaAPendienteYLimpiaRevisor() {
        Usuario host = anfitrion(20);
        Producto producto = productoConGrupos(host);
        producto.setEstadoAprobacion("APROBADO");
        producto.setComentarioRevision("Listo");
        producto.setRevisadoPor(admin(7));
        producto.setRevisadoAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO request = baseUpdate(producto);
        request.setNombre("Batido v2");
        request.setGruposOpciones(null);

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals("PENDIENTE", dto.getEstadoAprobacion());
        assertNull(dto.getRevisadoPorUsuarioId());
        assertNull(dto.getRevisadoAt());
        assertEquals("Listo", dto.getComentarioRevision());
        assertEquals(1, dto.getGruposOpciones().size());
    }

    @Test
    void rechazadoEdicionSigueRechazado() {
        Usuario host = anfitrion(20);
        Producto producto = productoConGrupos(host);
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO request = baseUpdate(producto);
        request.setDescripcion("Corregido");
        request.setGruposOpciones(null);

        ProductoDTO dto = productoService.updateProducto(10, request, 20);
        assertEquals("RECHAZADO", dto.getEstadoAprobacion());
        assertEquals("Faltan ingredientes", dto.getComentarioRevision());
    }

    @Test
    void putSinCambioRealDeAprobadoSigueAprobado() {
        Usuario host = anfitrion(20);
        Producto producto = productoConGrupos(host);
        producto.setEstadoAprobacion("APROBADO");
        producto.setComentarioRevision("Ok");
        producto.setRevisadoPor(admin(7));
        producto.setRevisadoAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO request = baseUpdate(producto);
        request.setGruposOpciones(null);

        ProductoDTO dto = productoService.updateProducto(10, request, 20);

        assertEquals("APROBADO", dto.getEstadoAprobacion());
        assertEquals(7, dto.getRevisadoPorUsuarioId());
        assertEquals("Ok", dto.getComentarioRevision());
    }

    @Test
    void adminVeGruposEnGetInternoYPendientes() {
        Producto producto = productoConGrupos(anfitrion(20));
        producto.setEstadoAprobacion("PENDIENTE");
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(productoRepository.findByEstadoAprobacion("PENDIENTE")).thenReturn(List.of(producto));

        ProductoDTO detalle = productoService.getProducto(10);
        assertEquals(1, detalle.getGruposOpciones().size());
        assertEquals("Sabores", detalle.getGruposOpciones().get(0).getNombre());
        assertEquals(List.of("Frutilla", "Vainilla"),
                detalle.getGruposOpciones().get(0).getOpciones()
                        .stream().map(ProductoOpcionDTO::getNombre).toList());

        List<ProductoDTO> bandeja = productoService.getProductosPendientes();
        assertEquals(1, bandeja.get(0).getGruposOpciones().size());
    }

    @Test
    void socioPublicoNoRecibeGruposNiRevision() {
        Producto producto = productoConGrupos(anfitrion(20));
        producto.setEstadoAprobacion("APROBADO");
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));

        ProductoDTO dto = productoService.getProductoPublico(10);

        assertNull(dto.getGruposOpciones());
        assertNull(dto.getIngredientes());
        assertNull(dto.getComentarioRevision());
    }

    @Test
    void ordenDeGruposYOpcionesEsDeterminista() {
        Usuario host = anfitrion(20);
        Producto producto = productoLocal(host, "APROBADO");
        ProductoGrupoOpcion segundo = grupoEntity(producto, 20, "B", 1);
        segundo.getOpciones().add(opcionEntity(segundo, 2, "Z", 1));
        segundo.getOpciones().add(opcionEntity(segundo, 1, "A", 0));
        ProductoGrupoOpcion primero = grupoEntity(producto, 10, "A", 0);
        primero.getOpciones().add(opcionEntity(primero, 5, "Y", 1));
        primero.getOpciones().add(opcionEntity(primero, 4, "X", 0));
        producto.getGruposOpciones().add(segundo);
        producto.getGruposOpciones().add(primero);
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));

        ProductoDTO dto = productoService.getProducto(10);

        assertEquals(List.of("A", "B"),
                dto.getGruposOpciones().stream().map(ProductoGrupoOpcionDTO::getNombre).toList());
        assertEquals(List.of("X", "Y"),
                dto.getGruposOpciones().get(0).getOpciones().stream().map(ProductoOpcionDTO::getNombre).toList());
        assertEquals(List.of("A", "Z"),
                dto.getGruposOpciones().get(1).getOpciones().stream().map(ProductoOpcionDTO::getNombre).toList());
    }

    private void stubCreateAnfitrion() {
        Usuario host = anfitrion(20);
        Hub hub = hub();
        Club club = clubDelAnfitrion(host);
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(host));
        when(hubRepository.findById(1)).thenReturn(Optional.of(hub));
        when(clubRepository.findByAnfitrionId(20)).thenReturn(List.of(club));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(10);
            }
            int gid = 1;
            int oid = 1;
            if (p.getGruposOpciones() != null) {
                for (ProductoGrupoOpcion g : p.getGruposOpciones()) {
                    if (g.getId() == null) {
                        g.setId(gid++);
                    }
                    if (g.getOpciones() != null) {
                        for (ProductoOpcion o : g.getOpciones()) {
                            if (o.getId() == null) {
                                o.setId(oid++);
                            }
                        }
                    }
                }
            }
            return p;
        });
        when(clubProductoRepository.findByClubIdAndProductoId(5, 10)).thenReturn(Optional.empty());
        when(clubProductoRepository.save(any(ClubProducto.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ProductoDTO baseCreate() {
        ProductoDTO request = new ProductoDTO();
        request.setNombre("Batido");
        request.setDescripcion("Proteico");
        request.setIngredientes("proteína");
        request.setPuntosValor(10);
        request.setPrecio(BigDecimal.ZERO);
        return request;
    }

    private static ProductoDTO baseUpdate(Producto producto) {
        ProductoDTO request = new ProductoDTO();
        request.setNombre(producto.getNombre());
        request.setDescripcion(producto.getDescripcion());
        request.setIngredientes(producto.getIngredientes());
        request.setImagenUrl(producto.getImagenUrl());
        request.setPuntosValor(producto.getPuntosValor());
        request.setPrecio(producto.getPrecio());
        return request;
    }

    private static ProductoGrupoOpcionDTO grupo(
            String nombre, int orden, int min, Integer max, boolean repetir, ProductoOpcionDTO... opciones) {
        ProductoGrupoOpcionDTO dto = new ProductoGrupoOpcionDTO();
        dto.setNombre(nombre);
        dto.setOrden(orden);
        dto.setMinSelecciones(min);
        dto.setMaxSelecciones(max);
        dto.setPermiteRepetir(repetir);
        dto.setOpciones(new ArrayList<>(List.of(opciones)));
        return dto;
    }

    private static ProductoOpcionDTO opcion(String nombre, int orden) {
        ProductoOpcionDTO dto = new ProductoOpcionDTO();
        dto.setNombre(nombre);
        dto.setOrden(orden);
        dto.setActivo(false);
        return dto;
    }

    private static Producto productoConGrupos(Usuario host) {
        Producto producto = productoLocal(host, "RECHAZADO");
        producto.setComentarioRevision("Faltan ingredientes");
        producto.setRevisadoPor(admin(7));
        producto.setRevisadoAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        ProductoGrupoOpcion grupo = grupoEntity(producto, 1, "Sabores", 0);
        grupo.setMinSelecciones(1);
        grupo.setMaxSelecciones(2);
        grupo.setPermiteRepetir(true);
        grupo.getOpciones().add(opcionEntity(grupo, 1, "Frutilla", 0));
        grupo.getOpciones().add(opcionEntity(grupo, 2, "Vainilla", 1));
        producto.getGruposOpciones().add(grupo);
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

    private static Producto productoLocal(Usuario host, String estado) {
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
        producto.setEstadoAprobacion(estado);
        producto.setActivo(true);
        producto.setGruposOpciones(new ArrayList<>());
        return producto;
    }

    private static Usuario admin(int id) {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        usuario.setNombre("Ana");
        usuario.setApellido("Admin");
        return usuario;
    }

    private static Usuario anfitrion(int id) {
        Rol rol = new Rol();
        rol.setNombre("ANFITRION");
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        usuario.setNombre("Andrea");
        usuario.setApellido("Anfitriona");
        usuario.setEmail("host-" + id + "@club.com");
        return usuario;
    }

    private static Hub hub() {
        Hub hub = new Hub();
        hub.setId(1);
        hub.setNombre("HUB Santa Cruz");
        return hub;
    }

    private static Club clubDelAnfitrion(Usuario host) {
        Club club = new Club();
        club.setId(5);
        club.setNombreClub("Club Demo");
        club.setAnfitrion(host);
        club.setHub(hub());
        return club;
    }
}
