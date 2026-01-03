package com.example.herbalife_clubes.dtos.club;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClubDTO {
    private Integer id;
    private Integer hubId;
    private String hubNombre;
    private Integer anfitrionId;
    private String anfitrionNombre;
    private String nombreClub;
    private String direccion;
    private String horario;
    private BigDecimal lat;
    private BigDecimal lng;
    private String estado;
    private LocalDateTime createdAt;
}

