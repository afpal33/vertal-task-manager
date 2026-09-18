package com.fabrizioroot.vertal.dto;
import jakarta.validation.constraints.NotBlank;
public record ChallengeRequestDto(@NotBlank String identificadorDispositivo) {}