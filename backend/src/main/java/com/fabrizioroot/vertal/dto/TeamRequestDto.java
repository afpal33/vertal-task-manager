package com.fabrizioroot.vertal.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record TeamRequestDto(@NotBlank @Size(max=120) String nombre) {}