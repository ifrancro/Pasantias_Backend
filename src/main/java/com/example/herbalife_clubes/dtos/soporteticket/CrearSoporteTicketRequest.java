package com.example.herbalife_clubes.dtos.soporteticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearSoporteTicketRequest {

    @NotBlank(message = "tipoSolicitud es requerido")
    @Size(max = 255, message = "tipoSolicitud no puede exceder 255 caracteres")
    private String tipoSolicitud;

    @NotBlank(message = "asunto es requerido")
    @Size(max = 255, message = "asunto no puede exceder 255 caracteres")
    private String asunto;

    @NotBlank(message = "mensaje es requerido")
    @Size(max = 2000, message = "mensaje no puede exceder 2000 caracteres")
    private String mensaje;
}
