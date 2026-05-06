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
    private String observaciones;
    private List<PedidoItemDTO> items;
}
