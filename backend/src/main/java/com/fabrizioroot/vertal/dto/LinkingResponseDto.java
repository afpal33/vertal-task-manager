package com.fabrizioroot.vertal.dto;
import com.fabrizioroot.vertal.model.EstadoSolicitud;
import java.time.Instant;
public record LinkingResponseDto(Long id, String nombreUsuarioSolicitado, EstadoSolicitud estado, Instant fechaSolicitud, String clavePublicaServidor) {}