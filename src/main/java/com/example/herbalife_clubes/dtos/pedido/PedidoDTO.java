package com.example.herbalife_clubes.dtos.pedido;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    private String horarioDeseado;
    private String tipoConsumo; // PARA_LLEVAR | EN_LUGAR
    private String observaciones;
    private String estado; // RECIBIDO | PREPARANDO | LISTO | ENTREGADO | CANCELADO
    private LocalDateTime fechaPedido;
}

