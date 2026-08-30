package com.example.herbalife_clubes.dtos.pedido;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoComboRequestDTO {
    private Integer comboId;
    private Integer cantidad;
    private List<PedidoComboComponenteRequestDTO> componentes = new ArrayList<>();
}
