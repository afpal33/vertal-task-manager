package com.fabrizioroot.vertal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor
public class Tarea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 180) private String titulo;
    @Column(columnDefinition = "text") private String descripcion;
    @Column(nullable = false) private Instant fechaCreacion = Instant.now();
    private Instant fechaVencimiento;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EstadoTarea estado = EstadoTarea.PENDIENTE;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Equipo equipo;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Usuario creador;
    @Column(nullable = false) private boolean activa = true;
}