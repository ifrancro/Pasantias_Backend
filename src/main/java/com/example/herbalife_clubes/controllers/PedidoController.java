package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoConItemsDTO;
import com.example.herbalife_clubes.services.PedidoService;
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

    @PostMapping("/con-items")
    public ResponseEntity<PedidoDTO> createPedidoConItems(@RequestBody PedidoConItemsDTO pedidoDTO,
                                                           @RequestParam Integer membresiaId,
                                                           @RequestParam Integer clubId) {
        PedidoDTO savedPedidoDTO = pedidoService.createPedidoConItems(pedidoDTO, membresiaId, clubId);
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
        List<PedidoDTO> pedidos = pedidoService.getPedidosByClub(clubId);
        return ResponseEntity.ok(pedidos);
    }

    /**
     * Actualiza el estado de un pedido.
     * Si el estado es 'PREPARANDO', el campo tiempo_estimado_minutos es obligatorio.
     * 
     * @param id ID del pedido
     * @param estado Nuevo estado (RECIBIDO | PREPARANDO | LISTO | ENTREGADO | CANCELADO)
     * @param tiempoEstimadoMinutos Tiempo estimado en minutos (obligatorio si estado es PREPARANDO)
     * @return PedidoDTO actualizado
     */
    @PatchMapping("{id}/estado")
    public ResponseEntity<PedidoDTO> actualizarEstado(
            @PathVariable Integer id, 
            @RequestParam String estado,
            @RequestParam(required = false) Integer tiempoEstimadoMinutos) {
        PedidoDTO pedidoDTO = pedidoService.actualizarEstado(id, estado, tiempoEstimadoMinutos);
        return ResponseEntity.ok(pedidoDTO);
    }

    @PatchMapping("{id}/cancelar")
    public ResponseEntity<PedidoDTO> cancelarPedido(@PathVariable Integer id) {
        PedidoDTO pedidoDTO = pedidoService.cancelarPedido(id);
        return ResponseEntity.ok(pedidoDTO);
    }
}

