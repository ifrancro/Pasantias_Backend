package com.example.herbalife_clubes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "logros")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Logro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_creador_id", nullable = true)
    private Club clubCreador;

    @Column(name = "nombre", length = 255)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "icono_url", length = 255)
    private String iconoUrl;

    @Column(name = "tipo_requisito")
    private Integer tipoRequisito;

    @Column(name = "tipo_metrica", length = 50)
    private String tipoMetrica;

    @Column(name = "meta_cantidad")
    private Integer metaCantidad;

    @Column(name = "puntos_recompensa")
    private Integer puntosRecompensa;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "estado_aprobacion", length = 50)
    private String estadoAprobacion;
}

