package com.fabrizioroot.vertal.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiration;
    public JwtService(@Value("${vertal.jwt.secret}") String secret, @Value("${vertal.jwt.expiration-ms:3600000}") long expiration) {
        if (secret.length() < 32) throw new IllegalArgumentException("vertal.jwt.secret debe tener al menos 32 caracteres");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expiration = expiration;
    }
    public String createToken(Long userId, String deviceId) { Instant now = Instant.now(); return Jwts.builder().subject(userId.toString()).claim("device_id", deviceId).issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(expiration))).signWith(key).compact(); }
    public Long userId(String token) { return Long.valueOf(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject()); }
    public String deviceId(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("device_id", String.class); }
}
