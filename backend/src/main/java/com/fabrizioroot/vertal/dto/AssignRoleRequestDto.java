package com.fabrizioroot.vertal.dto;
import com.fabrizioroot.vertal.model.Rol;
import jakarta.validation.constraints.NotNull;
public record AssignRoleRequestDto(@NotNull Rol rol) {}