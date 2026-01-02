package com.example.herbalife_clubes.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String message;
    private String segmentationType; // "tipo_segmentacion"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_club_id")
    private Club targetClub;

    private LocalDateTime sentAt;
}