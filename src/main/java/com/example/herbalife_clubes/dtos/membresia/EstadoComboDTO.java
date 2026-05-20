package com.example.herbalife_clubes.dtos.membresia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoComboDTO {
    private boolean haConsumidoCombo;
    private long totalCombosConsumidos;
}
