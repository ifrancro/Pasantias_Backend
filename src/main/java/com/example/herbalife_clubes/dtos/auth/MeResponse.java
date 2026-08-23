package com.example.herbalife_clubes.dtos.auth;

import com.example.herbalife_clubes.entities.Usuario;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Contrato público de GET /api/auth/me.
 * Nunca serializa la entidad JPA ni campos de UserDetails/credenciales.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MeResponse {

    private Integer userId;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private LocalDate fechaNacimiento;
    private String redesSociales;
    private String rolNombre;
    private String estado;

    public static MeResponse from(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return MeResponse.builder()
                .userId(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .fechaNacimiento(usuario.getFechaNacimiento())
                .redesSociales(usuario.getRedesSociales())
                .rolNombre(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                .estado(usuario.getEstado())
                .build();
    }
}
