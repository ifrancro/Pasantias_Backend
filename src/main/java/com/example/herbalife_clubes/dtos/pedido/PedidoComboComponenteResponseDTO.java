package com.example.herbalife_clubes.dtos.pedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoComboComponenteResponseDTO {
    private Integer productoId;
    private String productoNombre;
    private Integer cantidad;
    private List<PedidoItemOpcionResponseDTO> opciones = new ArrayList<>();
}
