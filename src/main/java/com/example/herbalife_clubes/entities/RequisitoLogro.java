package com.example.herbalife_clubes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "requisitos_logro")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequisitoLogro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logro_id", nullable = false)
    private Logro logro;

    @Column(name = "tipo_metrica", nullable = false, length = 50)
    private String tipoMetrica;

    @Column(name = "cantidad_esperada", nullable = false)
    private Integer cantidadEsperada;
}
