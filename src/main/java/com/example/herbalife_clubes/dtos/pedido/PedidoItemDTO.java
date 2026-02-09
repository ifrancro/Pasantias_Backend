package com.example.herbalife_clubes.dtos.pedido;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoItemDTO {
    private Integer productoId;
    private Integer cantidad;
    private String nota; // Nota específica del item (opcional)
}

