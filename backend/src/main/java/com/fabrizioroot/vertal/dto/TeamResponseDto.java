package com.fabrizioroot.vertal.dto;
import java.time.Instant;
public record TeamResponseDto(Long id, String nombre, Instant fechaCreacion, Long creadorId) {}