package com.example.herbalife_clubes.dtos.pedido;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoConItemsDTO {
    private String horarioDeseado;
    private String tipoConsumo; // PARA_LLEVAR | EN_LUGAR
    private String observaciones;
    private List<PedidoItemDTO> items; // Lista de items del pedido
}

