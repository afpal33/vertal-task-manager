package com.fabrizioroot.vertal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class ServidorAutogestionado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nombre;
    @Column(length = 255)
    private String direccionRed;
    @Column(nullable = false, columnDefinition = "text")
    private String clavePublicaServidor;
    @Column(nullable = false)
    private boolean activo = true;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Organizacion organizacion;
}