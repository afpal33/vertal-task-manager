package com.fabrizioroot.vertal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"tarea_id", "usuario_asignado_id"}))
public class Asignacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Tarea tarea;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Usuario usuarioAsignado;
    @Column(nullable = false) private Instant fechaAsignacion = Instant.now();
}