package com.example.herbalife_clubes.dtos.pedido;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoItemDTO {
    private Integer productoId;
    private String productoNombre; // Nombre del producto para facilitar visualización
    private Integer cantidad;
    private String nota; // Nota específica del item (opcional)
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}

