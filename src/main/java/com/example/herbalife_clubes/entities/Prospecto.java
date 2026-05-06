package com.example.herbalife_clubes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "prospectos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prospecto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "telefono", nullable = false, length = 50)
    private String telefono;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referido_por_membresia_id")
    private Membresia referidoPorMembresia;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    @Column(name = "estado", nullable = false, length = 50)
    private String estado;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDate.now();
        }
        if (estado == null || estado.isBlank()) {
            estado = "EN_SEGUIMIENTO";
        }
    }
}
