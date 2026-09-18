package com.fabrizioroot.vertal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor
public class VinculacionDispositivo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Usuario usuario;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Dispositivo dispositivo;
    @OneToOne(optional = false, fetch = FetchType.LAZY) private SolicitudVinculacion solicitudVinculacion;
    @Column(nullable = false) private Instant fechaAprobacion = Instant.now();
    @Column(nullable = false) private boolean activa = true;
}