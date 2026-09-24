package com.fabrizioroot.vertal.dto;

public record DeviceResponseDto(
        Long id,
        String nombre,
        String identificador,
        String nombreUsuario,
        boolean activo) {}
