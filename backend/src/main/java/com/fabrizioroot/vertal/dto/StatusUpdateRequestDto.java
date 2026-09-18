package com.fabrizioroot.vertal.dto;
import com.fabrizioroot.vertal.model.EstadoTarea;
import jakarta.validation.constraints.NotNull;
public record StatusUpdateRequestDto(@NotNull EstadoTarea estado) {}