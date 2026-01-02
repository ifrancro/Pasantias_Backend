package com.example.herbalife_clubes.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "memberships")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Membership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user; // Socio

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id")
    private Level level;

    @Column(unique = true)
    private String memberNumber; // "numero_socio"

    private String referredBy;
    private String howDidYouKnow;
    private Integer accumulatedPoints;

    @OneToMany(mappedBy = "membership", fetch = FetchType.LAZY)
    private List<Attendance> attendances;
}