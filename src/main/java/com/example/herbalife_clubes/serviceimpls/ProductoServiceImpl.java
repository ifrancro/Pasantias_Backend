package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoConDisponibilidadDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.ProductoMapper;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.ProductoRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.ProductoService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductoServiceImpl implements ProductoService {
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private HubRepository hubRepository;
    @Autowired
    private ClubProductoRepository clubProductoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public ProductoDTO createProducto(ProductoDTO productoDTO, Integer usuarioId, Integer hubId) {
        // Obtener usuario para determinar su rol
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
        
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        
        // Validar que el hub existe
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResourceNotFoundException("Hub no encontrado con id: " + hubId));
        
        // Crear producto desde el DTO
        Producto producto = ProductoMapper.mapProductoDTOToProducto(productoDTO);
        producto.setHub(hub);
        validarPrecio(producto.getPrecio());
        // esCombo es una categoría independiente del origen (GLOBAL/LOCAL): respetar lo enviado.
        producto.setEsCombo(Boolean.TRUE.equals(productoDTO.getEsCombo()));
        
        Club clubAnfitrion = null;
        // Lógica según el rol
        if ("ADMIN".equalsIgnoreCase(rolNombre)) {
            // ADMIN: Producto GLOBAL (club_creador_id = null, estado = APROBADO)
            producto.setClubCreador(null);
            producto.setTipo("GLOBAL");
            producto.setEstadoAprobacion("APROBADO");
        } else if ("ANFITRION".equalsIgnoreCase(rolNombre)) {
            // ANFITRION: Producto LOCAL (club_creador_id = club del anfitrión, estado = PENDIENTE)
            List<Club> clubes = clubRepository.findByAnfitrionId(usuarioId);
            if (clubes.isEmpty()) {
                throw new IllegalArgumentException("El anfitrión no tiene un club asociado");
            }
            clubAnfitrion = clubes.get(0);
            producto.setClubCreador(clubAnfitrion);
            producto.setTipo("LOCAL");
            producto.setEstadoAprobacion("PENDIENTE");
        } else {
            throw new IllegalArgumentException("Solo usuarios ADMIN o ANFITRION pueden crear productos");
        }
        
        // Valores por defecto
        if (producto.getActivo() == null) {
            producto.setActivo(true);
        }
        if (producto.getPuntosValor() == null) {
            producto.setPuntosValor(0);
        }
        if (producto.getPrecio() == null) {
            producto.setPrecio(BigDecimal.ZERO);
        }
        
        // Guardar producto
        Producto savedProducto = productoRepository.save(producto);

        // Producto LOCAL: crear entrada en club_productos con disponible=true para que anfitrión y socios lo vean
        if (clubAnfitrion != null) {
            final Club club = clubAnfitrion;
            clubProductoRepository.findByClubIdAndProductoId(club.getId(), savedProducto.getId())
                    .orElseGet(() -> {
                        ClubProducto cp = new ClubProducto();
                        cp.setClub(club);
                        cp.setProducto(savedProducto);
                        cp.setDisponible(true);
                        return clubProductoRepository.save(cp);
                    });
        }

        return ProductoMapper.mapProductoToProductoDTO(savedProducto);
    }

    /**
     * Método legacy - mantener para compatibilidad
     * @deprecated Usar createProducto en su lugar
     */
    @Deprecated
    @Transactional
    public ProductoDTO createProductoLegacy(ProductoDTO productoDTO, Integer clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        Hub hub = club.getHub();
        if (hub == null) {
            throw new IllegalArgumentException("El club no tiene HUB asociado");
        }

        Producto producto = ProductoMapper.mapProductoDTOToProducto(productoDTO);
        producto.setHub(hub);
        if (producto.getActivo() == null) {
            producto.setActivo(true);
        }
        
        Producto savedProducto = productoRepository.save(producto);

        // Crear relación ClubProducto automáticamente (comportamiento legacy)
        clubProductoRepository.findByClubIdAndProductoId(clubId, savedProducto.getId())
                .orElseGet(() -> {
                    ClubProducto cp = new ClubProducto();
                    cp.setClub(club);
                    cp.setProducto(savedProducto);
                    cp.setDisponible(false);
                    return clubProductoRepository.save(cp);
                });

        return ProductoMapper.mapProductoToProductoDTO(savedProducto);
    }

    @Override
    @Transactional
    public ProductoDTO createProductoFromHub(ProductoDTO productoDTO, Integer hubId) {
        // Validar que el hub existe
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResourceNotFoundException("Hub no encontrado con id: " + hubId));

        // Crear producto desde el DTO
        Producto producto = ProductoMapper.mapProductoDTOToProducto(productoDTO);
        producto.setHub(hub);
        validarPrecio(producto.getPrecio());
        producto.setEsCombo(Boolean.TRUE.equals(productoDTO.getEsCombo()));
        
        // Si no viene activo, por defecto true
        if (producto.getActivo() == null) {
            producto.setActivo(true);
        }
        if (producto.getPrecio() == null) {
            producto.setPrecio(BigDecimal.ZERO);
        }
        
        // Guardar producto (NO se crea relación ClubProducto automáticamente)
        // Los clubs verán el producto en GET /api/productos/hub/{hubId}?clubId={clubId}
        // y podrán habilitarlo usando el toggle
        Producto savedProducto = productoRepository.save(producto);

        return ProductoMapper.mapProductoToProductoDTO(savedProducto);
    }

    @Override
    @Transactional
    public ProductoDTO updateProducto(Integer productoId, ProductoDTO productoDTO, Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        assertPuedeEditarProducto(usuario, producto);

        producto.setNombre(productoDTO.getNombre());
        producto.setDescripcion(productoDTO.getDescripcion());
        producto.setImagenUrl(productoDTO.getImagenUrl());
        if (esAdmin(usuarioRol(usuario)) && productoDTO.getActivo() != null) {
            producto.setActivo(productoDTO.getActivo());
        }
        validarPrecio(productoDTO.getPrecio());
        producto.setPrecio(productoDTO.getPrecio() != null ? productoDTO.getPrecio() : BigDecimal.ZERO);
        producto.setPuntosValor(productoDTO.getPuntosValor() != null ? productoDTO.getPuntosValor() : 0);
        producto.setIngredientes(productoDTO.getIngredientes());
        if (productoDTO.getEsCombo() != null) {
            producto.setEsCombo(productoDTO.getEsCombo());
        }
        // RECHAZADO no pasa a PENDIENTE aquí: el anfitrión usa /reenviar.
        // APROBADO no vuelve a revisión en este ticket (PROD-OPTIONS-001b).

        productoRepository.save(producto);
        return ProductoMapper.mapProductoToProductoDTO(producto, true);
    }

    private void validarPrecio(BigDecimal precio) {
        if (precio != null && precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoDTO getProducto(Integer productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        return ProductoMapper.mapProductoToProductoDTO(producto, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoDTO getProductoPublico(Integer productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));

        if (!esVisibleEnMenuPublico(producto)) {
            throw new ResourceNotFoundException("Producto no encontrado con id: " + productoId);
        }

        return ProductoMapper.mapProductoToProductoDTO(producto, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> getProductos() {
        List<Producto> productos = productoRepository.findAll();
        return productos.stream()
                .map(p -> ProductoMapper.mapProductoToProductoDTO(p, true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> getProductosPublicos() {
        List<Producto> productos = productoRepository.findAll().stream()
                .filter(ProductoServiceImpl::esVisibleEnMenuPublico)
                .collect(Collectors.toList());

        return productos.stream()
                .map(p -> ProductoMapper.mapProductoToProductoDTO(p, false))
                .collect(Collectors.toList());
    }

    /**
     * Incluye producto si no existe fila en club_productos o si existe y disponible=true (opt-out).
     * Deuda PROD-AVAIL-002: el pedido socio exige fila explícita; el menú no.
     */
    private List<Producto> filtrarPorDisponibilidadEnClub(List<Producto> productos, Integer clubId) {
        return productos.stream()
                .filter(p -> {
                    Optional<ClubProducto> cp = clubProductoRepository.findByClubIdAndProductoId(clubId, p.getId());
                    return cp.isEmpty() || Boolean.TRUE.equals(cp.get().getDisponible());
                })
                .collect(Collectors.toList());
    }

    /**
     * Productos del menú del club: GLOBALES (hub, APROBADO) + LOCALES (club_creador_id=clubId, APROBADO).
     * Sin filtrar por disponibilidad (para uso interno cuando se rellena el DTO.disponible).
     */
    private List<Producto> obtenerProductosMenuClubSinFiltrarDisponibilidad(Integer clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        Integer hubId = club.getHub() != null ? club.getHub().getId() : null;
        if (hubId == null) {
            throw new ResourceNotFoundException("El club no tiene Hub asociado");
        }
        List<Producto> globales = productoRepository.findByHubIdAndTipoAndEstadoAprobacion(hubId, "GLOBAL", "APROBADO");
        List<Producto> locales = productoRepository.findByClubCreadorIdAndTipoAndEstadoAprobacion(clubId, "LOCAL", "APROBADO");
        List<Producto> todos = new java.util.ArrayList<>(globales);
        todos.addAll(locales);
        return todos;
    }

    /**
     * Productos del menú del club filtrando por disponibilidad (para socios y vista pública).
     * Requiere APROBADO + activo=true. Respeta opt-out de club_productos (PROD-AVAIL-002).
     */
    private List<Producto> obtenerProductosMenuClub(Integer clubId) {
        return filtrarPorDisponibilidadEnClub(
                obtenerProductosMenuClubSinFiltrarDisponibilidad(clubId).stream()
                        .filter(ProductoServiceImpl::esVisibleEnMenuPublico)
                        .collect(Collectors.toList()),
                clubId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> getProductosByClub(Integer clubId) {
        List<Producto> productos = obtenerProductosMenuClub(clubId);
        return productos.stream()
                .map(p -> ProductoMapper.mapProductoToProductoDTO(p, true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> getProductosByClubParaAnfitrion(Integer clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        Integer hubId = club.getHub() != null ? club.getHub().getId() : null;
        if (hubId == null) {
            throw new ResourceNotFoundException("El club no tiene Hub asociado");
        }
        List<Producto> globales = productoRepository.findByHubIdAndTipoAndEstadoAprobacion(hubId, "GLOBAL", "APROBADO");
        List<Producto> locales = productoRepository.findByClubCreadorId(clubId);
        List<Producto> todos = new java.util.ArrayList<>(globales);
        todos.addAll(locales);

        return todos.stream()
                .map(p -> {
                    ProductoDTO dto = ProductoMapper.mapProductoToProductoDTO(p, true);
                    Optional<ClubProducto> cp = clubProductoRepository.findByClubIdAndProductoId(clubId, p.getId());
                    dto.setDisponible(cp.map(ClubProducto::getDisponible).orElse(null));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> getProductosByClubPublico(Integer clubId) {
        List<Producto> productos = obtenerProductosMenuClub(clubId);
        return productos.stream()
                .map(p -> ProductoMapper.mapProductoToProductoDTO(p, false)) // Sin ingredientes
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> getProductosByClubAndTipo(Integer clubId, String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return getProductosByClub(clubId);
        }
        List<Producto> productos = obtenerProductosMenuClub(clubId).stream()
                .filter(p -> tipo.equalsIgnoreCase(p.getTipo()))
                .collect(Collectors.toList());
        return productos.stream()
                .map(p -> ProductoMapper.mapProductoToProductoDTO(p, true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> getProductosByClubAndTipoParaAnfitrion(Integer clubId, String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return getProductosByClubParaAnfitrion(clubId);
        }
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        Integer hubId = club.getHub() != null ? club.getHub().getId() : null;
        if (hubId == null) {
            throw new ResourceNotFoundException("El club no tiene Hub asociado");
        }
        List<Producto> globales = productoRepository.findByHubIdAndTipoAndEstadoAprobacion(hubId, "GLOBAL", "APROBADO");
        List<Producto> locales = productoRepository.findByClubCreadorId(clubId);
        List<Producto> todos = new java.util.ArrayList<>(globales);
        todos.addAll(locales);

        List<Producto> filtrados = todos.stream()
                .filter(p -> tipo.equalsIgnoreCase(p.getTipo()))
                .collect(Collectors.toList());

        return filtrados.stream()
                .map(p -> {
                    ProductoDTO dto = ProductoMapper.mapProductoToProductoDTO(p, true);
                    Optional<ClubProducto> cp = clubProductoRepository.findByClubIdAndProductoId(clubId, p.getId());
                    dto.setDisponible(cp.map(ClubProducto::getDisponible).orElse(null));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> getProductosByClubPublicoAndTipo(Integer clubId, String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return getProductosByClubPublico(clubId);
        }
        List<Producto> productos = obtenerProductosMenuClub(clubId).stream()
                .filter(p -> tipo.equalsIgnoreCase(p.getTipo()))
                .collect(Collectors.toList());
        return productos.stream()
                .map(p -> ProductoMapper.mapProductoToProductoDTO(p, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductoDTO cambiarEstadoAprobacion(
            Integer productoId, String estadoAprobacion, String comentario, Integer adminUsuarioId) {
        if (estadoAprobacion == null || estadoAprobacion.isBlank()) {
            throw new IllegalArgumentException("El estado de aprobación debe ser APROBADO o RECHAZADO");
        }
        String estado = estadoAprobacion.trim().toUpperCase();
        if (!"APROBADO".equals(estado) && !"RECHAZADO".equals(estado)) {
            throw new IllegalArgumentException("El estado de aprobación debe ser APROBADO o RECHAZADO");
        }

        Usuario admin = usuarioRepository.findById(adminUsuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + adminUsuarioId));
        if (!esAdmin(usuarioRol(admin))) {
            throw new AccessDeniedException("Solo un administrador puede revisar productos");
        }

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));

        String comentarioTrim = comentario != null ? comentario.trim() : "";
        if ("RECHAZADO".equals(estado)) {
            if (comentarioTrim.isEmpty()) {
                throw new IllegalArgumentException("El comentario es obligatorio al rechazar un producto");
            }
            producto.setComentarioRevision(comentarioTrim);
        } else {
            producto.setComentarioRevision(comentarioTrim.isEmpty() ? null : comentarioTrim);
        }

        producto.setEstadoAprobacion(estado);
        producto.setRevisadoPor(admin);
        producto.setRevisadoAt(LocalDateTime.now());
        productoRepository.save(producto);
        return ProductoMapper.mapProductoToProductoDTO(producto, true);
    }

    @Override
    @Transactional
    public ProductoDTO reenviarProducto(Integer productoId, Integer anfitrionUsuarioId) {
        Usuario usuario = usuarioRepository.findById(anfitrionUsuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + anfitrionUsuarioId));
        if (!esAnfitrion(usuarioRol(usuario))) {
            throw new AccessDeniedException("Solo el anfitrión propietario puede reenviar un producto");
        }

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));

        if (!"LOCAL".equalsIgnoreCase(producto.getTipo())) {
            throw new IllegalArgumentException("Solo se puede reenviar un producto LOCAL");
        }
        if (!esPropietarioLocal(usuario, producto)) {
            throw new AccessDeniedException("No puedes reenviar un producto de otro club");
        }
        if (!"RECHAZADO".equalsIgnoreCase(producto.getEstadoAprobacion())) {
            throw new IllegalArgumentException("Solo se puede reenviar un producto RECHAZADO");
        }

        producto.setEstadoAprobacion("PENDIENTE");
        producto.setRevisadoPor(null);
        producto.setRevisadoAt(null);
        productoRepository.save(producto);
        return ProductoMapper.mapProductoToProductoDTO(producto, true);
    }

    @Override
    @Transactional
    public ProductoDTO activarProducto(Integer productoId, Integer adminUsuarioId) {
        assertAdminCatalogo(adminUsuarioId);
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        producto.setActivo(true);
        Producto updatedProducto = productoRepository.save(producto);
        return ProductoMapper.mapProductoToProductoDTO(updatedProducto);
    }

    @Override
    @Transactional
    public ProductoDTO desactivarProducto(Integer productoId, Integer adminUsuarioId) {
        assertAdminCatalogo(adminUsuarioId);
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        producto.setActivo(false);
        Producto updatedProducto = productoRepository.save(producto);
        return ProductoMapper.mapProductoToProductoDTO(updatedProducto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> getProductosPendientes() {
        return productoRepository.findByEstadoAprobacion("PENDIENTE").stream()
                .map(p -> ProductoMapper.mapProductoToProductoDTO(p, true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> getProductosAprobados(Integer clubId) {
        List<Producto> productos = clubId != null
                ? productoRepository.findByEstadoAprobacionAndClubCreadorId("APROBADO", clubId)
                : productoRepository.findByEstadoAprobacion("APROBADO");
        return productos.stream()
                .map(p -> ProductoMapper.mapProductoToProductoDTO(p, true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> getProductosRechazados(Integer clubId) {
        List<Producto> productos = clubId != null
                ? productoRepository.findByEstadoAprobacionAndClubCreadorId("RECHAZADO", clubId)
                : productoRepository.findByEstadoAprobacion("RECHAZADO");
        return productos.stream()
                .map(p -> ProductoMapper.mapProductoToProductoDTO(p, true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoConDisponibilidadDTO> getProductosByHub(Integer hubId, Integer clubId) {
        System.out.println("[DEBUG] getProductosByHub - hubId: " + hubId + ", clubId: " + clubId);
        
        // Obtener todos los productos del Hub (sin filtrar por activo ni disponibilidad)
        List<Producto> productos = productoRepository.findByHubId(hubId);
        System.out.println("[DEBUG] Total de productos encontrados en Hub " + hubId + ": " + productos.size());
        
        List<ProductoConDisponibilidadDTO> resultado = productos.stream()
                .map(producto -> {
                    // Buscar si existe relación en club_productos
                    Boolean disponible = null;
                    if (clubId != null) {
                        Optional<ClubProducto> clubProductoOpt = 
                                clubProductoRepository.findByClubIdAndProductoId(clubId, producto.getId());
                        disponible = clubProductoOpt.map(ClubProducto::getDisponible).orElse(null);
                        System.out.println("[DEBUG] Producto ID: " + producto.getId() + ", Nombre: " + producto.getNombre() + 
                                         ", Disponible: " + disponible + " (null = sin relación)");
                    } else {
                        System.out.println("[DEBUG] Producto ID: " + producto.getId() + ", Nombre: " + producto.getNombre() + 
                                         ", Disponible: null (no clubId)");
                    }
                    
                    return ProductoConDisponibilidadDTO.builder()
                            .id(producto.getId())
                            .hubId(producto.getHub() != null ? producto.getHub().getId() : null)
                            .hubNombre(producto.getHub() != null ? producto.getHub().getNombre() : null)
                            .nombre(producto.getNombre())
                            .descripcion(producto.getDescripcion())
                            .tipo(producto.getTipo())
                            .esCombo(Boolean.TRUE.equals(producto.getEsCombo()))
                            .estadoAprobacion(producto.getEstadoAprobacion())
                            .activo(producto.getActivo())
                            .disponible(disponible) // null si no hay relación, true/false si existe
                            .createdAt(producto.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
        
        System.out.println("[DEBUG] Total de productos en respuesta: " + resultado.size());
        return resultado;
    }

    @Override
    @Transactional
    public ProductoConDisponibilidadDTO toggleDisponibilidadEnClub(Integer clubId, Integer productoId) {
        System.out.println("[DEBUG] toggleDisponibilidadEnClub - clubId: " + clubId + ", productoId: " + productoId);
        
        // Validar que el club existe
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> {
                    System.out.println("[ERROR] Club no encontrado con id: " + clubId);
                    return new ResourceNotFoundException("Club no encontrado con id: " + clubId);
                });
        
        System.out.println("[DEBUG] Club encontrado: " + club.getNombreClub());
        
        // Validar que el producto existe
        System.out.println("[DEBUG] Buscando producto con id: " + productoId);
        System.out.println("[DEBUG] Tipo de productoId: " + productoId.getClass().getName());
        
        // Intentar buscar el producto
        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        
        if (productoOpt.isEmpty()) {
            System.out.println("[ERROR] Producto no encontrado con id: " + productoId);
            // Listar todos los productos del hub para debug
            List<Producto> productosDelHub = productoRepository.findByHubId(club.getHub().getId());
            System.out.println("[DEBUG] Total de productos en el Hub " + club.getHub().getId() + ": " + productosDelHub.size());
            System.out.println("[DEBUG] Productos disponibles en el Hub:");
            productosDelHub.forEach(p -> System.out.println("  - ID: " + p.getId() + " (tipo: " + p.getId().getClass().getName() + "), Nombre: " + p.getNombre()));
            
            // Verificar si hay algún producto con ID similar (por si hay problema de tipo)
            productosDelHub.stream()
                .filter(p -> p.getId().toString().equals(productoId.toString()))
                .findFirst()
                .ifPresentOrElse(
                    p -> System.out.println("[DEBUG] ¡ENCONTRADO! Producto con ID como String coincide: " + p.getId()),
                    () -> System.out.println("[DEBUG] No se encontró ningún producto con ID que coincida como String")
                );
            
            throw new ResourceNotFoundException("Producto no encontrado con id: " + productoId + ". Verifica que el producto exista en el Hub " + club.getHub().getId());
        }
        
        Producto producto = productoOpt.get();
        
        System.out.println("[DEBUG] Producto encontrado: " + producto.getNombre() + " (ID: " + producto.getId() + ")");

        if (!"APROBADO".equalsIgnoreCase(producto.getEstadoAprobacion())) {
            throw new IllegalArgumentException(
                    "Solo se puede cambiar la disponibilidad de un producto APROBADO");
        }
        if ("LOCAL".equalsIgnoreCase(producto.getTipo())) {
            Integer creadorId = producto.getClubCreador() != null ? producto.getClubCreador().getId() : null;
            if (creadorId == null || !clubId.equals(creadorId)) {
                throw new AccessDeniedException(
                        "No puedes cambiar la disponibilidad de un producto local de otro club");
            }
        }
        
        // Validar que el producto pertenece al mismo Hub que el club
        if (!producto.getHub().getId().equals(club.getHub().getId())) {
            throw new IllegalArgumentException("El producto no pertenece al mismo Hub que el club");
        }
        
        // Buscar o crear la relación en club_productos
        ClubProducto clubProducto = clubProductoRepository
                .findByClubIdAndProductoId(clubId, productoId)
                .orElseGet(() -> {
                    ClubProducto nuevo = new ClubProducto();
                    nuevo.setClub(club);
                    nuevo.setProducto(producto);
                    nuevo.setDisponible(false); // Por defecto false
                    return nuevo;
                });
        
        // Toggle del estado disponible (solo en club_productos, NO toca el activo global)
        clubProducto.setDisponible(!clubProducto.getDisponible());
        clubProducto = clubProductoRepository.save(clubProducto);
        
        // Construir y devolver el DTO
        return ProductoConDisponibilidadDTO.builder()
                .id(producto.getId())
                .hubId(producto.getHub() != null ? producto.getHub().getId() : null)
                .hubNombre(producto.getHub() != null ? producto.getHub().getNombre() : null)
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .tipo(producto.getTipo())
                .esCombo(Boolean.TRUE.equals(producto.getEsCombo()))
                .estadoAprobacion(producto.getEstadoAprobacion())
                .activo(producto.getActivo()) // Estado global (no se modifica)
                .disponible(clubProducto.getDisponible()) // Estado local en el club
                .createdAt(producto.getCreatedAt())
                .build();
    }

    private void assertAdminCatalogo(Integer adminUsuarioId) {
        Usuario usuario = usuarioRepository.findById(adminUsuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + adminUsuarioId));
        if (!esAdmin(usuarioRol(usuario))) {
            throw new AccessDeniedException("Solo un administrador puede activar o desactivar un producto");
        }
    }

    private static boolean esVisibleEnMenuPublico(Producto producto) {
        return "APROBADO".equalsIgnoreCase(producto.getEstadoAprobacion())
                && Boolean.TRUE.equals(producto.getActivo());
    }

    private void assertPuedeEditarProducto(Usuario usuario, Producto producto) {
        String rol = usuarioRol(usuario);
        if (esAdmin(rol)) {
            return;
        }
        if (esAnfitrion(rol) && esPropietarioLocal(usuario, producto)) {
            return;
        }
        throw new AccessDeniedException("No tienes permiso para editar este producto");
    }

    private static boolean esPropietarioLocal(Usuario usuario, Producto producto) {
        if (!"LOCAL".equalsIgnoreCase(producto.getTipo())) {
            return false;
        }
        Club club = producto.getClubCreador();
        if (club == null || club.getAnfitrion() == null || usuario.getId() == null) {
            return false;
        }
        return usuario.getId().equals(club.getAnfitrion().getId());
    }

    private static String usuarioRol(Usuario usuario) {
        return usuario.getRol() != null ? usuario.getRol().getNombre() : "";
    }

    private static boolean esAdmin(String rol) {
        return "ADMIN".equalsIgnoreCase(rol);
    }

    private static boolean esAnfitrion(String rol) {
        return "ANFITRION".equalsIgnoreCase(rol);
    }
}

