package com.sneaky.sneaky.security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
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
    private static final long ACCESS_TOKEN_EXPIRY = 1000 * 60 * 15; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRY = 1000 * 60 * 60 * 24 * 7; // 7 days

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    // NEW: Generate token with claims (including role)
    public String generateAccessToken(UUID userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId.toString());
        return generateToken(claims, userId.toString(), ACCESS_TOKEN_EXPIRY);
    }

    public String generateAccessToken(UUID userId) {
        return generateAccessToken(userId, "USER");
    }

    public String generateRefreshToken(UUID userId) {
        return generateToken(new HashMap<>(), userId.toString(), REFRESH_TOKEN_EXPIRY);
    }
    
    public String generateAdminToken(UUID userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId.toString());
        claims.put("isAdmin", true);
        return generateToken(claims, userId.toString(), ACCESS_TOKEN_EXPIRY);
    }

    private String generateToken(Map<String, Object> claims, String subject, long expiryTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiryTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
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
}