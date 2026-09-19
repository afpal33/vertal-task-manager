package com.fabrizioroot.vertal.dto;
import com.fabrizioroot.vertal.model.EstadoTarea;
import java.time.Instant;
public record TaskResponseDto(Long id, String titulo, String descripcion, Instant fechaCreacion, Instant fechaVencimiento, EstadoTarea estado, Long equipoId, Long creadorId, Long asignadoId, String asignadoNombreUsuario, String asignadoNombreCompleto) {}