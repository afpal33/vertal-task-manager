package com.fabrizioroot.vertal.dto;
import jakarta.validation.constraints.NotBlank;
public record AssignmentRequestDto(@NotBlank String nombreUsuario) {}