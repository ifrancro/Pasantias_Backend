package com.example.herbalife_clubes.dtos.prospecto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProspectoEstadoUpdateRequest {
    @NotBlank(message = "estado es requerido")
    private String estado;
}
