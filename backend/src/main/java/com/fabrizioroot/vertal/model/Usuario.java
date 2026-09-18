package com.fabrizioroot.vertal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 80)
    private String nombreUsuario;
    @Column(nullable = false, length = 160)
    private String nombreCompleto;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Rol rol = Rol.USUARIO_NORMAL;
    @Column(nullable = false)
    private boolean activo = true;
    @Column(nullable = false)
    private Instant fechaRegistro = Instant.now();
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Organizacion organizacion;
}