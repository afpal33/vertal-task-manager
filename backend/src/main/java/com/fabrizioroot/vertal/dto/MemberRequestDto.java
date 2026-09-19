package com.fabrizioroot.vertal.dto;
import jakarta.validation.constraints.NotBlank;
public record MemberRequestDto(@NotBlank String nombreUsuario) {}