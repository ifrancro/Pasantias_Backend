package com.example.herbalife_clubes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "misiones_prospecto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MisionProspecto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospecto_id", nullable = false)
    private Prospecto prospecto;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "meta_cantidad", nullable = false)
    private Integer metaCantidad;

    @Column(name = "progreso_actual", nullable = false)
    private Integer progresoActual;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @PrePersist
    protected void onCreate() {
        if (progresoActual == null) {
            progresoActual = 0;
        }
    }
}
