package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
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
    private NotificacionRepository notificacionRepository;

    @Override
    @Transactional
    public PedidoDTO createPedido(PedidoDTO pedidoDTO, Integer membresiaId, Integer clubId, Integer productoId) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
        
        Pedido pedido = PedidoMapper.mapPedidoDTOToPedido(pedidoDTO);
        pedido.setMembresia(membresia);
        pedido.setClub(club);
        pedido.setProducto(producto);
        if (pedido.getEstado() == null) {
            pedido.setEstado("PENDIENTE");
        }
        
        Pedido savedPedido = pedidoRepository.save(pedido);
        
        // Crear notificación para el anfitrión del club
        Usuario anfitrion = club.getAnfitrion();
        if (anfitrion != null) {
            Notificacion notificacion = new Notificacion();
            notificacion.setTitulo("Nuevo Pedido");
            notificacion.setMensaje("El socio " + membresia.getNumeroSocio() + " ha realizado un pedido de " + 
                    producto.getNombre() + " en tu club " + club.getNombreClub());
            notificacion.setTipoSegmentacion("PEDIDO");
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
        List<Pedido> pedidos = pedidoRepository.findByMembresiaId(membresiaId);
        return pedidos.stream()
                .map(PedidoMapper::mapPedidoToPedidoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PedidoDTO> getPedidosByClub(Integer clubId) {
        List<Pedido> pedidos = pedidoRepository.findByClubId(clubId);
        return pedidos.stream()
                .map(PedidoMapper::mapPedidoToPedidoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PedidoDTO actualizarEstado(Integer pedidoId, String estado) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId));
        pedido.setEstado(estado);
        Pedido updatedPedido = pedidoRepository.save(pedido);
        return PedidoMapper.mapPedidoToPedidoDTO(updatedPedido);
    }

    @Override
    public PedidoDTO cancelarPedido(Integer pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId));
        
        if ("ENTREGADO".equalsIgnoreCase(pedido.getEstado()) || "CANCELADO".equalsIgnoreCase(pedido.getEstado())) {
            throw new IllegalArgumentException("No se puede cancelar un pedido que ya está " + pedido.getEstado());
        }
        
        pedido.setEstado("CANCELADO");
        Pedido updatedPedido = pedidoRepository.save(pedido);
        return PedidoMapper.mapPedidoToPedidoDTO(updatedPedido);
    }
}

