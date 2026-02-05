package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.services.PedidoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin("*")
public class PedidoController {
    @Autowired
    private PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoDTO> createPedido(@RequestBody PedidoDTO pedidoDTO,
                                                   @RequestParam Integer membresiaId,
                                                   @RequestParam Integer clubId,
                                                   @RequestParam Integer productoId) {
        PedidoDTO savedPedidoDTO = pedidoService.createPedido(pedidoDTO, membresiaId, clubId, productoId);
        return new ResponseEntity<>(savedPedidoDTO, HttpStatus.CREATED);
    }

    @GetMapping("{id}")
    public ResponseEntity<PedidoDTO> getPedido(@PathVariable Integer id) {
        PedidoDTO pedidoDTO = pedidoService.getPedido(id);
        return ResponseEntity.ok(pedidoDTO);
    }

    @GetMapping("/socio/{membresiaId}")
    public ResponseEntity<List<PedidoDTO>> getPedidosBySocio(@PathVariable Integer membresiaId) {
        List<PedidoDTO> pedidos = pedidoService.getPedidosBySocio(membresiaId);
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<PedidoDTO>> getPedidosByClub(@PathVariable Integer clubId) {
        System.out.println("[JSON DEBUG] ===== GET /api/pedidos/club/" + clubId + " =====");
        System.out.println("[JSON DEBUG] clubId recibido: " + clubId);
        
        List<PedidoDTO> pedidos = pedidoService.getPedidosByClub(clubId);
        
        // Imprimir JSON real
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(pedidos);
            System.out.println("[JSON DEBUG] JSON Response (GET /api/pedidos/club/" + clubId + "):");
            System.out.println(json);
            System.out.println("[JSON DEBUG] Cantidad de pedidos en JSON: " + pedidos.size());
            
            // Imprimir detalles de cada pedido
            if (!pedidos.isEmpty()) {
                System.out.println("[JSON DEBUG] Detalles de campos por pedido:");
                for (int i = 0; i < pedidos.size(); i++) {
                    PedidoDTO p = pedidos.get(i);
                    System.out.println("[JSON DEBUG] Pedido #" + (i + 1) + ":");
                    System.out.println("  - id: " + p.getId() + " (tipo: " + (p.getId() != null ? p.getId().getClass().getSimpleName() : "null") + ")");
                    System.out.println("  - membresiaId: " + p.getMembresiaId() + " (tipo: " + (p.getMembresiaId() != null ? p.getMembresiaId().getClass().getSimpleName() : "null") + ")");
                    System.out.println("  - membresiaNumeroSocio: " + p.getMembresiaNumeroSocio() + " (tipo: " + (p.getMembresiaNumeroSocio() != null ? p.getMembresiaNumeroSocio().getClass().getSimpleName() : "null") + ")");
                    System.out.println("  - clubId: " + p.getClubId() + " (tipo: " + (p.getClubId() != null ? p.getClubId().getClass().getSimpleName() : "null") + ")");
                    System.out.println("  - clubNombre: " + p.getClubNombre() + " (tipo: " + (p.getClubNombre() != null ? p.getClubNombre().getClass().getSimpleName() : "null") + ")");
                    System.out.println("  - productoId: " + p.getProductoId() + " (tipo: " + (p.getProductoId() != null ? p.getProductoId().getClass().getSimpleName() : "null") + ")");
                    System.out.println("  - productoNombre: " + p.getProductoNombre() + " (tipo: " + (p.getProductoNombre() != null ? p.getProductoNombre().getClass().getSimpleName() : "null") + ")");
                    System.out.println("  - cantidad: " + p.getCantidad() + " (tipo: " + (p.getCantidad() != null ? p.getCantidad().getClass().getSimpleName() : "null") + ")");
                    System.out.println("  - horarioDeseado: " + p.getHorarioDeseado() + " (tipo: " + (p.getHorarioDeseado() != null ? p.getHorarioDeseado().getClass().getSimpleName() : "null") + ")");
                    System.out.println("  - tipoConsumo: " + p.getTipoConsumo() + " (tipo: " + (p.getTipoConsumo() != null ? p.getTipoConsumo().getClass().getSimpleName() : "null") + ")");
                    System.out.println("  - observaciones: " + p.getObservaciones() + " (tipo: " + (p.getObservaciones() != null ? p.getObservaciones().getClass().getSimpleName() : "null") + ")");
                    System.out.println("  - estado: " + p.getEstado() + " (tipo: " + (p.getEstado() != null ? p.getEstado().getClass().getSimpleName() : "null") + ")");
                    System.out.println("  - fechaPedido: " + p.getFechaPedido() + " (tipo: " + (p.getFechaPedido() != null ? p.getFechaPedido().getClass().getSimpleName() : "null") + ")");
                }
            } else {
                System.out.println("[JSON DEBUG] LISTA VACÍA - No hay pedidos para mostrar");
            }
            
            System.out.println("[JSON DEBUG] ===== FIN GET /api/pedidos/club/" + clubId + " =====");
        } catch (Exception e) {
            System.out.println("[JSON DEBUG] ERROR al serializar JSON: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(pedidos);
    }

    @PatchMapping("{id}/estado")
    public ResponseEntity<PedidoDTO> actualizarEstado(@PathVariable Integer id, @RequestParam String estado) {
        PedidoDTO pedidoDTO = pedidoService.actualizarEstado(id, estado);
        return ResponseEntity.ok(pedidoDTO);
    }

    @PatchMapping("{id}/cancelar")
    public ResponseEntity<PedidoDTO> cancelarPedido(@PathVariable Integer id) {
        PedidoDTO pedidoDTO = pedidoService.cancelarPedido(id);
        return ResponseEntity.ok(pedidoDTO);
    }
}

