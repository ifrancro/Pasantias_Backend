package com.example.herbalife_clubes.dtos.membresia;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CompraManualCreateRequest {
    @NotBlank(message = "descripcion es requerida")
    private String descripcion;

    @NotNull(message = "monto es requerido")
    @DecimalMin(value = "0.01", message = "monto debe ser mayor a 0")
    private BigDecimal monto;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
}
