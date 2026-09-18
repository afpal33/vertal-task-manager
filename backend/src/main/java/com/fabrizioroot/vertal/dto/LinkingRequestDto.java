package com.fabrizioroot.vertal.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record LinkingRequestDto(@NotBlank @Size(max=80) String nombreUsuarioSolicitado,
                                @NotBlank @Size(max=120) String nombreDispositivo,
                                @NotBlank @Size(max=160) String identificadorDispositivo,
                                @NotBlank String clavePublicaDispositivo) {}