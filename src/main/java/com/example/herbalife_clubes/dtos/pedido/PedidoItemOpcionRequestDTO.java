package com.example.herbalife_clubes.dtos.pedido;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoItemOpcionRequestDTO {
    private Integer grupoId;
    private Integer opcionId;
    /** Cuántas veces se eligió esta opción dentro del grupo (no confundir con cantidad del producto). */
    private Integer cantidad;
}
