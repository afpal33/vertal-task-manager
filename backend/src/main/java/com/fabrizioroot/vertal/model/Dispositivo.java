package com.fabrizioroot.vertal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Dispositivo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 160)
    private String identificadorDispositivo;
    @Column(nullable = false, length = 120)
    private String nombreDispositivo;
    @Column(nullable = false, columnDefinition = "text")
    private String clavePublicaDispositivo;
    @Column(nullable = false, columnDefinition = "text")
    private String clavePublicaServidor;
    @Column(nullable = false)
    private boolean activo = true;
    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario usuario;
}