package com.example.herbalife_clubes.dtos.notificacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionDTO {
    private Integer id;
    private String titulo;
    private String mensaje;
    private String tipoSegmentacion;
    private Integer hubId;
    private String hubNombre;
    private Integer clubId;
    private String clubNombre;
    private Integer usuarioId;
    private String usuarioNombre;
    private Integer pedidoId;
    private LocalDateTime fechaEnvio;
}

