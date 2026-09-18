package com.fabrizioroot.vertal.dto;
import jakarta.validation.constraints.NotBlank;
public record LoginRequestDto(@NotBlank String challengeId, @NotBlank String identificadorDispositivo, @NotBlank String firma) {}