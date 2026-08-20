package com.example.herbalife_clubes.dtos.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerifyEmailResponse {
    private boolean verified;
    private String message;
    private String token;
    private Integer userId;
    private String email;
    private String nombre;
    private String apellido;
    private String rolNombre;
}
