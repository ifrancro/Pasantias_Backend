package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.common.PagedResponse;
import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoConItemsDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoMostradorRequestDTO;
import com.example.herbalife_clubes.services.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
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

    @PostMapping("/mostrador")
    public ResponseEntity<PedidoDTO> createPedidoMostrador(@RequestBody PedidoMostradorRequestDTO request) {
        PedidoDTO savedPedidoDTO = pedidoService.createPedidoMostrador(request);
        return new ResponseEntity<>(savedPedidoDTO, HttpStatus.CREATED);
    }

    @GetMapping("{id}")
    public ResponseEntity<PedidoDTO> getPedido(@PathVariable Integer id) {
        PedidoDTO pedidoDTO = pedidoService.getPedido(id);
        return ResponseEntity.ok(pedidoDTO);
    }

    /**
     * Legacy: lista completa de pedidos del socio.
     * Preferir {@link #getPedidosBySocioPaginados}.
     */
    @GetMapping("/socio/{membresiaId}")
    public ResponseEntity<List<PedidoDTO>> getPedidosBySocio(@PathVariable Integer membresiaId) {
        List<PedidoDTO> pedidos = pedidoService.getPedidosBySocio(membresiaId);
        return ResponseEntity.ok(pedidos);
    }

    /**
     * Legacy: lista completa de pedidos del club.
     * Preferir {@link #getPedidosByClubPaginados}.
     */
    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<PedidoDTO>> getPedidosByClub(@PathVariable Integer clubId) {
        List<PedidoDTO> pedidos = pedidoService.getPedidosByClub(clubId);
        return ResponseEntity.ok(pedidos);
    }

    /**
     * Pedidos del socio con paginación real (metadata incluida).
     * page base 0, size default 20, máximo 100.
     */
    @GetMapping("/socio/{membresiaId}/paginados")
    public ResponseEntity<PagedResponse<PedidoDTO>> getPedidosBySocioPaginados(
            @PathVariable Integer membresiaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(
                pedidoService.getPedidosBySocioPaginados(membresiaId, page, size, estado, desde, hasta));
    }

    /**
     * Pedidos del club con paginación real (metadata incluida).
     * page base 0, size default 20, máximo 100.
     */
    @GetMapping("/club/{clubId}/paginados")
    public ResponseEntity<PagedResponse<PedidoDTO>> getPedidosByClubPaginados(
            @PathVariable Integer clubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(
                pedidoService.getPedidosByClubPaginados(clubId, page, size, estado, desde, hasta));
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

