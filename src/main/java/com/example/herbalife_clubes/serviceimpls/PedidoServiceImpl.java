package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoConItemsDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemDTO;
import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.PedidoMapper;
import com.example.herbalife_clubes.repositories.*;
import com.example.herbalife_clubes.services.PedidoService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PedidoServiceImpl implements PedidoService {
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private MembresiaRepository membresiaRepository;
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private ClubProductoRepository clubProductoRepository;
    @Autowired
    private NotificacionRepository notificacionRepository;

    @Override
    @Transactional
    public PedidoDTO createPedido(PedidoDTO pedidoDTO, Integer membresiaId, Integer clubId, Integer productoId) {
        System.out.println("[PEDIDO] ===== INICIO CREAR PEDIDO =====");
        System.out.println("[PEDIDO] Parámetros recibidos:");
        System.out.println("[PEDIDO]   - membresiaId: " + membresiaId);
        System.out.println("[PEDIDO]   - clubId: " + clubId);
        System.out.println("[PEDIDO]   - productoId: " + productoId);
        System.out.println("[PEDIDO]   - horarioDeseado: " + pedidoDTO.getHorarioDeseado());
        System.out.println("[PEDIDO]   - tipoConsumo: " + pedidoDTO.getTipoConsumo());
        System.out.println("[PEDIDO]   - cantidad: " + pedidoDTO.getCantidad());
        System.out.println("[PEDIDO]   - observaciones: " + pedidoDTO.getObservaciones());
        
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));

        System.out.println("[PEDIDO] Entidades encontradas:");
        System.out.println("[PEDIDO]   - Membresía ID: " + membresia.getId() + ", Número Socio: " + membresia.getNumeroSocio());
        System.out.println("[PEDIDO]   - Club ID: " + club.getId() + ", Nombre: " + club.getNombreClub());
        System.out.println("[PEDIDO]   - Producto ID: " + producto.getId() + ", Nombre: " + producto.getNombre());

        // Validar que el club destino esté activo
        System.out.println("[PEDIDO] Validando estado del club...");
        System.out.println("[PEDIDO]   - Club estado: " + club.getEstado());
        if (club.getEstado() == null || (!club.getEstado().equals("APROBADO") && !club.getEstado().equals("ACTIVO"))) {
            System.out.println("[PEDIDO] ERROR: Club no está activo. Estado: " + club.getEstado());
            throw new IllegalArgumentException("El club destino no está activo. Estado actual: " + club.getEstado());
        }
        System.out.println("[PEDIDO] ✓ Club válido");

        // Validar que el socio esté activo
        System.out.println("[PEDIDO] Validando estado de la membresía...");
        System.out.println("[PEDIDO]   - Membresía estado: " + membresia.getEstado());
        if (membresia.getEstado() == null || !membresia.getEstado().equals("ACTIVA")) {
            System.out.println("[PEDIDO] ERROR: Membresía no está activa. Estado: " + membresia.getEstado());
            throw new IllegalArgumentException("La membresía no está activa. Estado actual: " + membresia.getEstado());
        }
        System.out.println("[PEDIDO] ✓ Membresía válida");

        // Producto debe estar disponible en el club (validación de disponibilidad por club)
        System.out.println("[PEDIDO] Validando disponibilidad del producto en el club...");
        ClubProducto cp = clubProductoRepository.findByClubIdAndProductoId(clubId, productoId)
                .orElseThrow(() -> {
                    System.out.println("[PEDIDO] ERROR: Producto no está configurado para este club");
                    return new IllegalArgumentException("El producto no está configurado para este club");
                });
        System.out.println("[PEDIDO]   - ClubProducto encontrado, disponible: " + cp.getDisponible());
        if (cp.getDisponible() == null || !cp.getDisponible()) {
            System.out.println("[PEDIDO] ERROR: Producto no está disponible. Disponible: " + cp.getDisponible());
            throw new IllegalArgumentException("El producto no está disponible en este club");
        }
        System.out.println("[PEDIDO] ✓ Producto disponible");
        
        Pedido pedido = PedidoMapper.mapPedidoDTOToPedido(pedidoDTO);
        pedido.setMembresia(membresia);
        pedido.setClub(club);
        
        // Compatibilidad: pedido viejo era 1 producto. Creamos 1 item.
        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setProducto(producto);
        item.setCantidad(pedidoDTO.getCantidad() != null ? pedidoDTO.getCantidad() : 1);
        item.setNota(pedidoDTO.getObservaciones());
        pedido.getItems().add(item);
        
        // Asignar producto y cantidad directamente al pedido para compatibilidad con BD
        // (la BD tiene producto_id y cantidad en la tabla pedidos)
        pedido.setProducto(producto);
        pedido.setCantidad(pedidoDTO.getCantidad() != null ? pedidoDTO.getCantidad() : 1);

        // enums
        pedido.setEstado(EstadoPedido.RECIBIDO);
        try {
            if (pedidoDTO.getTipoConsumo() != null) {
                pedido.setTipoConsumo(TipoConsumo.valueOf(pedidoDTO.getTipoConsumo()));
            }
        } catch (Exception ignored) {
        }
        pedido.setHorarioDeseado(pedidoDTO.getHorarioDeseado());
        
        System.out.println("[PEDIDO] ANTES DE GUARDAR:");
        System.out.println("[PEDIDO]   - pedido.clubId: " + (pedido.getClub() != null ? pedido.getClub().getId() : "NULL"));
        System.out.println("[PEDIDO]   - pedido.membresiaId: " + (pedido.getMembresia() != null ? pedido.getMembresia().getId() : "NULL"));
        System.out.println("[PEDIDO]   - pedido.estado: " + pedido.getEstado());
        System.out.println("[PEDIDO]   - pedido.fechaPedido: " + pedido.getFechaPedido());
        System.out.println("[PEDIDO]   - items.size(): " + (pedido.getItems() != null ? pedido.getItems().size() : 0));
        
        Pedido savedPedido = pedidoRepository.save(pedido);
        
        System.out.println("[PEDIDO] DESPUÉS DE GUARDAR:");
        System.out.println("[PEDIDO]   - pedidoId: " + savedPedido.getId());
        System.out.println("[PEDIDO]   - membresiaId: " + (savedPedido.getMembresia() != null ? savedPedido.getMembresia().getId() : "NULL"));
        System.out.println("[PEDIDO]   - clubId: " + (savedPedido.getClub() != null ? savedPedido.getClub().getId() : "NULL"));
        System.out.println("[PEDIDO]   - productoId: " + (savedPedido.getItems() != null && !savedPedido.getItems().isEmpty() 
                ? savedPedido.getItems().get(0).getProducto().getId() : "NULL"));
        System.out.println("[PEDIDO]   - estado: " + savedPedido.getEstado());
        System.out.println("[PEDIDO]   - fechaPedido: " + savedPedido.getFechaPedido());
        System.out.println("[PEDIDO] ===== FIN CREAR PEDIDO =====");
        
        // Crear notificación para el anfitrión del club
        Usuario anfitrion = club.getAnfitrion();
        if (anfitrion != null) {
            Notificacion notificacion = new Notificacion();
            notificacion.setTitulo("Nuevo Pedido");
            notificacion.setMensaje("El socio " + membresia.getNumeroSocio() + " ha realizado un pedido de " + 
                    producto.getNombre() + " en tu club " + club.getNombreClub());
            notificacion.setTipoSegmentacion("pedido");
            notificacion.setClub(club);
            notificacion.setUsuario(anfitrion);
            notificacion.setPedido(savedPedido);
            notificacionRepository.save(notificacion);
        }
        
        return PedidoMapper.mapPedidoToPedidoDTO(savedPedido);
    }

    @Override
    @Transactional
    public PedidoDTO createPedidoConItems(PedidoConItemsDTO pedidoDTO, Integer membresiaId, Integer clubId) {
        System.out.println("[PEDIDO] ===== INICIO CREAR PEDIDO CON MÚLTIPLES ITEMS =====");
        System.out.println("[PEDIDO] Parámetros recibidos:");
        System.out.println("[PEDIDO]   - membresiaId: " + membresiaId);
        System.out.println("[PEDIDO]   - clubId: " + clubId);
        System.out.println("[PEDIDO]   - horarioDeseado: " + pedidoDTO.getHorarioDeseado());
        System.out.println("[PEDIDO]   - tipoConsumo: " + pedidoDTO.getTipoConsumo());
        System.out.println("[PEDIDO]   - observaciones: " + pedidoDTO.getObservaciones());
        System.out.println("[PEDIDO]   - items: " + (pedidoDTO.getItems() != null ? pedidoDTO.getItems().size() : 0));
        
        // Validar que hay items
        if (pedidoDTO.getItems() == null || pedidoDTO.getItems().isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un item");
        }
        
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));

        System.out.println("[PEDIDO] Entidades encontradas:");
        System.out.println("[PEDIDO]   - Membresía ID: " + membresia.getId() + ", Número Socio: " + membresia.getNumeroSocio());
        System.out.println("[PEDIDO]   - Club ID: " + club.getId() + ", Nombre: " + club.getNombreClub());

        // Validar que el club destino esté activo
        if (club.getEstado() == null || (!club.getEstado().equals("APROBADO") && !club.getEstado().equals("ACTIVO"))) {
            throw new IllegalArgumentException("El club destino no está activo. Estado actual: " + club.getEstado());
        }

        // Validar que el socio esté activo
        if (membresia.getEstado() == null || !membresia.getEstado().equals("ACTIVA")) {
            throw new IllegalArgumentException("La membresía no está activa. Estado actual: " + membresia.getEstado());
        }
        
        // Crear el pedido
        Pedido pedido = new Pedido();
        pedido.setMembresia(membresia);
        pedido.setClub(club);
        pedido.setHorarioDeseado(pedidoDTO.getHorarioDeseado());
        pedido.setObservaciones(pedidoDTO.getObservaciones());
        pedido.setEstado(EstadoPedido.RECIBIDO);
        pedido.setFechaPedido(LocalDateTime.now());
        
        try {
            if (pedidoDTO.getTipoConsumo() != null) {
                pedido.setTipoConsumo(TipoConsumo.valueOf(pedidoDTO.getTipoConsumo()));
            } else {
                pedido.setTipoConsumo(TipoConsumo.EN_LUGAR);
            }
        } catch (Exception e) {
            pedido.setTipoConsumo(TipoConsumo.EN_LUGAR);
        }
        
        // Validar y crear items
        Producto primerProducto = null;
        int cantidadTotal = 0;
        
        for (PedidoItemDTO itemDTO : pedidoDTO.getItems()) {
            Producto producto = productoRepository.findById(itemDTO.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + itemDTO.getProductoId()));
            
            // Validar disponibilidad del producto en el club
            ClubProducto cp = clubProductoRepository.findByClubIdAndProductoId(clubId, itemDTO.getProductoId())
                    .orElseThrow(() -> new IllegalArgumentException("El producto " + producto.getNombre() + " no está configurado para este club"));
            
            if (cp.getDisponible() == null || !cp.getDisponible()) {
                throw new IllegalArgumentException("El producto " + producto.getNombre() + " no está disponible en este club");
            }
            
            // Crear item
            PedidoItem item = new PedidoItem();
            item.setPedido(pedido);
            item.setProducto(producto);
            item.setCantidad(itemDTO.getCantidad() != null ? itemDTO.getCantidad() : 1);
            item.setNota(itemDTO.getNota());
            pedido.getItems().add(item);
            
            // Guardar referencia al primer producto para compatibilidad con BD
            if (primerProducto == null) {
                primerProducto = producto;
            }
            cantidadTotal += (itemDTO.getCantidad() != null ? itemDTO.getCantidad() : 1);
        }
        
        // Asignar producto y cantidad del primer item para compatibilidad con BD
        if (primerProducto != null) {
            pedido.setProducto(primerProducto);
            pedido.setCantidad(cantidadTotal);
        }
        
        System.out.println("[PEDIDO] ANTES DE GUARDAR:");
        System.out.println("[PEDIDO]   - items.size(): " + pedido.getItems().size());
        System.out.println("[PEDIDO]   - cantidadTotal: " + cantidadTotal);
        
        Pedido savedPedido = pedidoRepository.save(pedido);
        
        System.out.println("[PEDIDO] DESPUÉS DE GUARDAR:");
        System.out.println("[PEDIDO]   - pedidoId: " + savedPedido.getId());
        System.out.println("[PEDIDO]   - items.size(): " + (savedPedido.getItems() != null ? savedPedido.getItems().size() : 0));
        System.out.println("[PEDIDO] ===== FIN CREAR PEDIDO CON MÚLTIPLES ITEMS =====");
        
        // Crear notificación para el anfitrión del club
        Usuario anfitrion = club.getAnfitrion();
        if (anfitrion != null) {
            Notificacion notificacion = new Notificacion();
            notificacion.setTitulo("Nuevo Pedido");
            StringBuilder mensaje = new StringBuilder("El socio " + membresia.getNumeroSocio() + " ha realizado un pedido con " + 
                    savedPedido.getItems().size() + " producto(s) en tu club " + club.getNombreClub());
            notificacion.setMensaje(mensaje.toString());
            notificacion.setTipoSegmentacion("pedido");
            notificacion.setClub(club);
            notificacion.setUsuario(anfitrion);
            notificacion.setPedido(savedPedido);
            notificacionRepository.save(notificacion);
        }
        
        return PedidoMapper.mapPedidoToPedidoDTO(savedPedido);
    }

    @Override
    public PedidoDTO getPedido(Integer pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId));
        return PedidoMapper.mapPedidoToPedidoDTO(pedido);
    }

    @Override
    public List<PedidoDTO> getPedidosBySocio(Integer membresiaId) {
        // Usar método con JOIN FETCH para cargar items y productos
        List<Pedido> pedidos = pedidoRepository.findByMembresiaIdWithRelations(membresiaId);
        return pedidos.stream()
                .map(PedidoMapper::mapPedidoToPedidoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PedidoDTO> getPedidosByClub(Integer clubId) {
        System.out.println("[PEDIDO] ===== INICIO LISTAR PEDIDOS POR CLUB =====");
        System.out.println("[PEDIDO] clubId recibido: " + clubId);
        
        // Obtener información del club para logging
        Club club = clubRepository.findById(clubId).orElse(null);
        if (club != null) {
            System.out.println("[PEDIDO] Club encontrado:");
            System.out.println("[PEDIDO]   - clubId: " + club.getId());
            System.out.println("[PEDIDO]   - clubNombre: " + club.getNombreClub());
            System.out.println("[PEDIDO]   - anfitrionId: " + (club.getAnfitrion() != null ? club.getAnfitrion().getId() : "NULL"));
        } else {
            System.out.println("[PEDIDO] WARNING: Club no encontrado con id: " + clubId);
        }
        
        // Usar método con JOIN FETCH para evitar problemas de Lazy Loading
        List<Pedido> pedidos = pedidoRepository.findByClubIdWithRelations(clubId);
        
        System.out.println("[PEDIDO] Pedidos encontrados: " + pedidos.size());
        if (!pedidos.isEmpty()) {
            System.out.println("[PEDIDO] Primeros 3 pedidos:");
            pedidos.stream().limit(3).forEach(p -> {
                System.out.println("[PEDIDO]   - Pedido ID: " + p.getId() + 
                        ", Estado: " + p.getEstado() + 
                        ", Fecha: " + p.getFechaPedido() +
                        ", Club ID: " + (p.getClub() != null ? p.getClub().getId() : "NULL") +
                        ", Membresía ID: " + (p.getMembresia() != null ? p.getMembresia().getId() : "NULL") +
                        ", Items: " + (p.getItems() != null ? p.getItems().size() : 0));
            });
        } else {
            System.out.println("[PEDIDO] LISTA VACÍA - No se encontraron pedidos para clubId: " + clubId);
        }
        
        List<PedidoDTO> pedidosDTO = pedidos.stream()
                .map(PedidoMapper::mapPedidoToPedidoDTO)
                .collect(Collectors.toList());
        
        System.out.println("[PEDIDO] DTOs generados: " + pedidosDTO.size());
        System.out.println("[PEDIDO] ===== FIN LISTAR PEDIDOS POR CLUB =====");
        
        return pedidosDTO;
    }

    @Override
    public PedidoDTO actualizarEstado(Integer pedidoId, String estado) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId));
        pedido.setEstado(EstadoPedido.valueOf(estado));
        Pedido updatedPedido = pedidoRepository.save(pedido);
        return PedidoMapper.mapPedidoToPedidoDTO(updatedPedido);
    }

    @Override
    public PedidoDTO cancelarPedido(Integer pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId));
        
        if (EstadoPedido.ENTREGADO.equals(pedido.getEstado()) || EstadoPedido.CANCELADO.equals(pedido.getEstado())) {
            throw new IllegalArgumentException("No se puede cancelar un pedido que ya está " + pedido.getEstado().name());
        }
        
        pedido.setEstado(EstadoPedido.CANCELADO);
        Pedido updatedPedido = pedidoRepository.save(pedido);
        return PedidoMapper.mapPedidoToPedidoDTO(updatedPedido);
    }
}

