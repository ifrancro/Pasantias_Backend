package com.example.herbalife_clubes.dtos.prospecto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProspectoCreateRequest {
    @NotBlank(message = "nombre es requerido")
    private String nombre;

    @NotBlank(message = "telefono es requerido")
    private String telefono;

    private Integer referidoPorMembresiaId;
}
