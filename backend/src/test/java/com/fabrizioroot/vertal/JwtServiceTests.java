package com.fabrizioroot.vertal;

import com.fabrizioroot.vertal.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTests {
    @Test
    void tokenIsBoundToUserAndDevice() {
        JwtService service = new JwtService(
                "test-secret-with-at-least-32-characters-long",
                60_000);

        String token = service.createToken(7L, "device-1");

        assertEquals(7L, service.userId(token));
        assertEquals("device-1", service.deviceId(token));
    }
}
