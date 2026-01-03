package com.example.herbalife_clubes.dtos.soporteticket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SoporteTicketDTO {
    private Integer id;
    private Integer usuarioId;
    private String usuarioNombre;
    private String tipoSolicitud;
    private String asunto;
    private String mensaje;
    private String estado;
    private String respuestaAdmin;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaRespuesta;
}

