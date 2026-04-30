package com.sneaky.sneaky.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil(
            Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)));

    @Test
    void generateAccessTokenCanBeParsedForSubjectAndDates() {
        String token = jwtUtil.generateAccessToken("dev@example.com");

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("dev@example.com");
        assertThat(jwtUtil.extractIssuedAt(token)).isBeforeOrEqualTo(jwtUtil.extractExpiration(token));
        assertThat(jwtUtil.extractExpiration(token)).isAfter(jwtUtil.extractIssuedAt(token));
    }

    @Test
    void invalidTokenIsRejected() {
        assertThatThrownBy(() -> jwtUtil.extractEmail("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }
}
