package com.fabrizioroot.vertal.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
public record TaskRequestDto(@NotBlank String titulo, String descripcion, Instant fechaVencimiento, @NotNull Long equipoId) {}