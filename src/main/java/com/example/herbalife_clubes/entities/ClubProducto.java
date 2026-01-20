package com.example.herbalife_clubes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "club_productos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"club_id", "producto_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClubProducto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "disponible")
    private Boolean disponible;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (disponible == null) {
            disponible = false;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}


