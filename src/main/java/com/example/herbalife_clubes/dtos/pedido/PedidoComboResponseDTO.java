package com.example.herbalife_clubes.dtos.pedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoComboResponseDTO {
    private Integer pedidoComboId;
    private Integer comboId;
    private String comboNombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private Integer puntosValor;
    private List<PedidoComboComponenteResponseDTO> items = new ArrayList<>();
}
