package com.example.herbalife_clubes.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;//Relaion estrict con la tabla roles

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;


    //Datos nulleables (de momento)
    private String phone; //telefonos
    private LocalDate birthDate; //fecha de cumpleaños
    private String socialMedia; //red_social

    //Relacion inversa para el anfitrion
    @OneToMany(mappedBy = "host", fetch = FetchType.LAZY)
    private List<Club> hostedClubs;

}
