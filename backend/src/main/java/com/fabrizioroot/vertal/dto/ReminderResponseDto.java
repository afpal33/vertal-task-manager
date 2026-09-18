package com.fabrizioroot.vertal.dto;
import java.time.Instant;
public record ReminderResponseDto(Long id, Long tareaId, Long usuarioId, Instant fechaHora, boolean activo) {}