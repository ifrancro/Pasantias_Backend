package com.example.herbalife_clubes.models;

import com.example.herbalife_clubes.models.PedidosApartado.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "clubs")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Club {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User host;

    @Column(nullable = false)
    private String name;

    private String address;
    private String schedule; // "horario"

    // Geolocalización
    private Double latitude;
    private Double longitude;

    private String status; // "PENDING", "ACTIVE", "INACTIVE"

    @OneToMany(mappedBy = "club", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ClubPhoto> photos;

    @OneToMany(mappedBy = "club", fetch = FetchType.LAZY)
    private List<Product> menu;
}
