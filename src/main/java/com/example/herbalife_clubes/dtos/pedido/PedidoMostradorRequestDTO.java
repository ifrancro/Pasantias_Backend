package com.example.herbalife_clubes.dtos.pedido;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoMostradorRequestDTO {
    private Integer clubId;
    private String socioCodigo; // numero_socio opcional
    private String tipoConsumo; // opcional: PARA_RECOGER | EN_LUGAR
    private String tipoPago; // EFECTIVO | TRANSFERENCIA | QR | TARJETA | OTRO
    private String observaciones;
    private List<PedidoItemDTO> items;
    /** Mismo contrato que POST /pedidos/con-items (deuda UI mostrador si aún no envía configuración). */
    private List<PedidoComboRequestDTO> combos;
}
