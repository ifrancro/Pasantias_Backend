package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
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

