package com.example.herbalife_clubes.dtos.prospecto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MisionProspectoCreateRequest {
    @NotBlank(message = "nombre es requerido")
    private String nombre;

    private String descripcion;

    @NotNull(message = "metaCantidad es requerido")
    @Min(value = 1, message = "metaCantidad debe ser >= 1")
    private Integer metaCantidad;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaLimite;
}
