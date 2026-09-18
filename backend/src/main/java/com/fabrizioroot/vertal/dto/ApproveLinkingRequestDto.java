package com.fabrizioroot.vertal.dto;
import jakarta.validation.constraints.NotBlank;
public record ApproveLinkingRequestDto(@NotBlank String nombreCompleto, String rol) {}