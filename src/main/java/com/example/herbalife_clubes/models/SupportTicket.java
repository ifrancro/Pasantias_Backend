package com.example.herbalife_clubes.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupportTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String subject;
    private String message;
    private String requestType; // "tipo_solicitud"
    private String status;      // "OPEN", "CLOSED"

    private String adminResponse;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}