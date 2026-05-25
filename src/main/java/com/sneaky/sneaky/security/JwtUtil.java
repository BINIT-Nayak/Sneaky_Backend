package com.sneaky.sneaky.security;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
    private final Key key;
    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final long ACCESS_TOKEN_EXPIRY = 1000 * 60 * 15; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRY = 1000 * 60 * 60 * 24 * 7; // 7 days

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateAccessToken(UUID userId, String role) {
        return generateToken(
                userId,
                ACCESS_TOKEN_EXPIRY,
                ACCESS_TOKEN_TYPE,
                null,
                Map.of("role", normalizeRole(role), "userId", userId.toString()));
    }

    public String generateAccessToken(UUID userId) {
        return generateAccessToken(userId, "USER");
    }

    public String generateRefreshToken(UUID userId) {
        return generateToken(
                userId,
                REFRESH_TOKEN_EXPIRY,
                REFRESH_TOKEN_TYPE,
                UUID.randomUUID().toString(),
                Map.of());
    }

    public String generateAdminToken(UUID userId, String role) {
        return generateToken(
                userId,
                ACCESS_TOKEN_EXPIRY,
                ACCESS_TOKEN_TYPE,
                null,
                Map.of("role", normalizeRole(role), "userId", userId.toString(), "isAdmin", true));
    }

    private String generateToken(
            UUID userId,
            long expiryTime,
            String tokenType,
            String tokenId,
            Map<String, Object> claims) {
        var builder = Jwts.builder()
                .setClaims(claims)
                .setSubject(userId.toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiryTime));

        if (tokenId != null) {
            builder.setId(tokenId);
        }

        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    public UUID extractUserId(String token) {
        String subject = getClaims(token).getSubject();
        return UUID.fromString(subject);
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public boolean isAdminToken(String token) {
        String role = extractRole(token);
        return "ADMIN".equals(role) || "MODERATOR".equals(role);
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Date extractExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    public Date extractIssuedAt(String token) {
        return getClaims(token).getIssuedAt();
    }

    public String extractTokenType(String token) {
        return getClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
    }

    public String extractTokenId(String token) {
        return getClaims(token).getId();
    }

    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(extractTokenType(token)) && extractTokenId(token) != null;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "USER";
        }

        String normalizedRole = role.trim().toUpperCase();
        if (normalizedRole.startsWith("ROLE_")) {
            return normalizedRole.substring("ROLE_".length());
        }

        return normalizedRole;
    }
}
