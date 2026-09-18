package com.fabrizioroot.vertal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor
public class Organizacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 120)
    private String nombre;
    @Column(nullable = false)
    private Instant fechaCreacion = Instant.now();
    @Column(nullable = false)
    private boolean activa = true;
}