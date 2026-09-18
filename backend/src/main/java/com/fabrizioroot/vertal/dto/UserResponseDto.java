package com.fabrizioroot.vertal.dto;
import com.fabrizioroot.vertal.model.Rol;
public record UserResponseDto(Long id, String nombreUsuario, String nombreCompleto, Rol rol, boolean activo) {}