package com.fabrizioroot.vertal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor
public class Equipo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String nombre;
    @Column(nullable = false) private Instant fechaCreacion = Instant.now();
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Usuario creador;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Organizacion organizacion;
    @Column(nullable = false) private boolean activo = true;
}