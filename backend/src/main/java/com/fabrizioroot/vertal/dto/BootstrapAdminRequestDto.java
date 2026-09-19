package com.fabrizioroot.vertal.dto;

import jakarta.validation.constraints.NotBlank;

public record BootstrapAdminRequestDto(
        @NotBlank String bootstrapToken,
        @NotBlank String nombreCompleto,
        @NotBlank String nombreDispositivo,
        @NotBlank String identificadorDispositivo,
        @NotBlank String clavePublicaDispositivo) {}