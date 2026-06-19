package com.example.herbalife_clubes.dtos.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroVentaDiariaDTO {
    private Integer numeroFila;
    private LocalDate fecha;
    private String hora;
    private String nombre;
    /** N = Nuevo, R = Referido, vacío = regular */
    private String estatusVisita;
    private String numeroSocio;
    @Builder.Default
    private List<ProductoVentaDiariaDTO> productos = new ArrayList<>();
    private String tipoPago;
    private BigDecimal totalBs;
    /** MOSTRADOR o SOCIO */
    private String origen;
    private Integer pedidoId;
}
