package com.fabrizioroot.vertal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "equipo_id"}))
public class MembresiaEquipo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Usuario usuario;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Equipo equipo;
    @Column(nullable = false) private Instant fechaIngreso = Instant.now();
    @Column(nullable = false) private boolean activa = true;
}