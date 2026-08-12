package com.company.ruanzhu.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
            "ruanzhu-secret-key-must-be-at-least-256-bits-long-for-hs256",
            86400000L
        );
    }

    @Test
    void generateAndValidateToken() {
        String token = tokenProvider.generateToken(1L, "testuser", "STAFF");

        assertTrue(tokenProvider.validateToken(token));
        assertEquals("testuser", tokenProvider.getUsernameFromToken(token));
        assertEquals(1L, tokenProvider.getUserIdFromToken(token));
        assertEquals("STAFF", tokenProvider.getRoleFromToken(token));
    }

    @Test
    void invalidToken_ReturnsFalse() {
        assertFalse(tokenProvider.validateToken("invalid-token"));
        assertFalse(tokenProvider.validateToken(null));
        assertFalse(tokenProvider.validateToken(""));
    }
}
