package com.example.herbalife_clubes.dtos.consumo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumoDTO {
    private Integer id;
    private Integer membresiaId;
    private String membresiaNumeroSocio;
    private Integer clubId;
    private String clubNombre;
    private Integer asistenciaId;
    private Integer pedidoId;
    private String descripcion;
    private LocalDateTime fechaHora;
    private LocalDateTime createdAt;
}

