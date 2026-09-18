package com.fabrizioroot.vertal.dto;
import java.time.Instant;
public record ChallengeResponseDto(String challengeId, String challenge, Instant expiresAt) {}