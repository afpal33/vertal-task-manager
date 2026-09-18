package com.fabrizioroot.vertal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor
public class SolicitudVinculacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String nombreUsuarioSolicitado;
    @Column(nullable = false, length = 120)
    private String nombreDispositivo;
    @Column(nullable = false, length = 160)
    private String identificadorDispositivo;
    @Column(nullable = false, columnDefinition = "text")
    private String clavePublicaDispositivo;
    @Column(nullable = false)
    private Instant fechaSolicitud = Instant.now();
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Organizacion organizacion;
}