package com.example.herbalife_clubes.dtos.pedido;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoConItemsDTO {
    private String horarioDeseado; // @deprecated - usar tiempoEstimadoMinutos
    private String tipoConsumo; // PARA_RECOGER | EN_LUGAR
    private Integer tiempoEstimadoMinutos; // Tiempo estimado de preparación en minutos
    private String observaciones;
    private List<PedidoItemDTO> items; // Productos sueltos
    /** Combos con composición fija y opciones configurables por componente. */
    private List<PedidoComboRequestDTO> combos;
}

