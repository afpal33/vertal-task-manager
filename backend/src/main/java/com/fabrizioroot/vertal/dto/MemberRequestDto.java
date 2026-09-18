package com.fabrizioroot.vertal.dto;
import jakarta.validation.constraints.NotNull;
public record MemberRequestDto(@NotNull Long usuarioId) {}