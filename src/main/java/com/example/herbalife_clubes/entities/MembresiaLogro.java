package com.example.herbalife_clubes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "membresia_logros",
       uniqueConstraints = @UniqueConstraint(columnNames = {"membresia_id", "logro_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembresiaLogro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membresia_id", nullable = false)
    private Membresia membresia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logro_id", nullable = false)
    private Logro logro;

    @Column(name = "fecha_obtencion")
    private LocalDateTime fechaObtencion;

    @PrePersist
    protected void onCreate() {
        if (fechaObtencion == null) {
            fechaObtencion = LocalDateTime.now();
        }
    }
}

