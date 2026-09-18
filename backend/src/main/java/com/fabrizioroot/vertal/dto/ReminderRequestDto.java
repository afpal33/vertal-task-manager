package com.fabrizioroot.vertal.dto;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
public record ReminderRequestDto(@NotNull @Future Instant fechaHora) {}