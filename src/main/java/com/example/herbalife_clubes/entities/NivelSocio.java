package com.example.herbalife_clubes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "niveles_socio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NivelSocio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", length = 255)
    private String nombre;

    @Column(name = "visitas_requeridas")
    private Integer visitasRequeridas;

    @Column(name = "descripcion_beneficios", columnDefinition = "TEXT")
    private String descripcionBeneficios;
}

