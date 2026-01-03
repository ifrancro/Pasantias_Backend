package com.example.herbalife_clubes.dtos.hub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HubDTO {
    private Integer id;
    private Integer adminId;
    private String adminNombre;
    private String nombre;
    private String descripcion;
    private String estado;
    private LocalDateTime createdAt;
}

