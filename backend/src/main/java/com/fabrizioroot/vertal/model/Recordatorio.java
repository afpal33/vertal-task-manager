package com.fabrizioroot.vertal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor
public class Recordatorio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Tarea tarea;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Usuario usuario;
    @Column(nullable = false) private Instant fechaHora;
    @Column(nullable = false) private boolean activo = true;
}