package com.example.herbalife_clubes.dtos.membresia;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArbolReferidosDTO {
    private Integer membresiaId;
    private String numeroSocio;
    private String nombreCompleto;
    private Integer puntosAcumulados;
    private String estado;
    private List<ArbolReferidosDTO> referidos; // Referidos directos (hijos en el árbol)

    public ArbolReferidosDTO(Integer membresiaId, String numeroSocio, String nombreCompleto, 
                            Integer puntosAcumulados, String estado) {
        this.membresiaId = membresiaId;
        this.numeroSocio = numeroSocio;
        this.nombreCompleto = nombreCompleto;
        this.puntosAcumulados = puntosAcumulados;
        this.estado = estado;
        this.referidos = new ArrayList<>();
    }

    public void agregarReferido(ArbolReferidosDTO referido) {
        if (this.referidos == null) {
            this.referidos = new ArrayList<>();
        }
        this.referidos.add(referido);
    }
}

