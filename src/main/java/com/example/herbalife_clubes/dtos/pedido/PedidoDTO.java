package com.example.herbalife_clubes.dtos.pedido;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {
    private Integer id;
    private Integer membresiaId;
    private String membresiaNumeroSocio;
    private Integer clubId;
    private String clubNombre;
    // Compatibilidad: antes existía productoId/cantidad (pedido de 1 item).
    private Integer productoId;
    private String productoNombre;
    private Integer cantidad;

    private String horarioDeseado; // @deprecated - usar tiempoEstimadoMinutos
    private String tipoConsumo; // PARA_RECOGER | EN_LUGAR
    private String tipoPago;
    private Integer tiempoEstimadoMinutos; // Tiempo estimado de preparación en minutos
    private String observaciones;
    private String estado; // RECIBIDO | PREPARANDO | LISTO | ENTREGADO | CANCELADO
    private LocalDateTime fechaPedido;
    
    // Lista de items del pedido (pedido_items)
    private List<PedidoItemDTO> items;
}

