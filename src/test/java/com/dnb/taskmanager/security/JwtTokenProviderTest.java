package com.dnb.taskmanager.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String SECRET = "unitTestSecretKeyThatIsAtLeast256BitsLongForHmacSha256AlgorithmTesting!";
    private static final long EXPIRATION_MS = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Should generate valid JWT token and extract subject")
    void generateToken_And_GetUserIdFromToken() {
        String email = "developer@example.com";
        String token = jwtTokenProvider.generateToken(email);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(email);
    }

    @Test
    @DisplayName("Should return false for invalid or tampered token")
    void validateToken_Invalid() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalidPayload.signature";
        assertThat(jwtTokenProvider.validateToken(invalidToken)).isFalse();
    }

    @Test
    @DisplayName("Should return false for expired token")
    void validateToken_Expired() {
        // Create token provider with negative expiration to simulate immediate expiry
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, -1000L);
        String token = expiredProvider.generateToken("user@example.com");

        assertThat(expiredProvider.validateToken(token)).isFalse();
    }
}

