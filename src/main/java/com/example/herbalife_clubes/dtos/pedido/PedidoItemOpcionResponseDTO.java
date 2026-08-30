package com.example.herbalife_clubes.dtos.pedido;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoItemOpcionResponseDTO {
    private Integer grupoId;
    private String grupoNombre;
    private Integer grupoOrden;
    private Integer opcionId;
    private String opcionNombre;
    private Integer opcionOrden;
    private Integer cantidad;
}
