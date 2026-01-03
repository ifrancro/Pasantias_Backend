package com.example.herbalife_clubes.dtos.evento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoDTO {
    private Integer id;
    private Integer hubId;
    private String hubNombre;
    private Integer clubId;
    private String clubNombre;
    private String nombre;
    private LocalDate fechaEvento;
    private String descripcion;
}

