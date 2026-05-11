package com.sneaky.sneaky.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil(
            Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)));

    @Test
    void generateAccessTokenCanBeParsedForSubjectAndDates() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId);

        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo(JwtUtil.ACCESS_TOKEN_TYPE);
        assertThat(jwtUtil.extractTokenId(token)).isNull();
        assertThat(jwtUtil.isAccessToken(token)).isTrue();
        assertThat(jwtUtil.isRefreshToken(token)).isFalse();
        assertThat(jwtUtil.extractIssuedAt(token)).isBeforeOrEqualTo(jwtUtil.extractExpiration(token));
        assertThat(jwtUtil.extractExpiration(token)).isAfter(jwtUtil.extractIssuedAt(token));
    }

    @Test
    void generateRefreshTokenHasRefreshTypeAndTokenId() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateRefreshToken(userId);

        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo(JwtUtil.REFRESH_TOKEN_TYPE);
        assertThat(jwtUtil.extractTokenId(token)).isNotBlank();
        assertThat(jwtUtil.isAccessToken(token)).isFalse();
        assertThat(jwtUtil.isRefreshToken(token)).isTrue();
    }

    @Test
    void invalidTokenIsRejected() {
        assertThatThrownBy(() -> jwtUtil.extractUserId("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }
}
