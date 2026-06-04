package com.sneaky.sneaky.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.auth.LoginRequestDTO;
import com.sneaky.sneaky.dto.auth.AuthTokensDTO;
import com.sneaky.sneaky.dto.auth.LogoutResponseDTO;
import com.sneaky.sneaky.dto.auth.RefreshResponseDTO;
import com.sneaky.sneaky.dto.user.CreateUserRequestDTO;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.repository.UsersRepository;
import com.sneaky.sneaky.security.JwtUtil;
import com.sneaky.sneaky.util.EmailNormalizer;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthService {

    private final UsersRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public AuthTokensDTO authenticate(LoginRequestDTO loginRequest) {
        String normalizedEmail = EmailNormalizer.normalize(loginRequest.getEmail());

        Users user = userRepository.findByEmail(normalizedEmail)
                .or(() -> userRepository.findByEmailIgnoreCase(normalizedEmail))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // Check if user is banned
        if (user.getIsBanned() != null && user.getIsBanned()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account has been banned. Please contact support.");
        }

        // Update last login
        user.setEmail(normalizedEmail);
        user.setLastLogin(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate token with role
        String accessToken = generateAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        return new AuthTokensDTO(accessToken, refreshToken, normalizeRole(user.getRole()));
    }

    public RefreshResponseDTO refresh(String refreshToken) {
        try {
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
            }

            var userId = jwtUtil.extractUserId(refreshToken);
            String tokenId = jwtUtil.extractTokenId(refreshToken);

            if (Boolean.TRUE
                    .equals(redisTemplate.hasKey(refreshTokenBlacklistKey(tokenId)))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
            }

            String logoutTimestamp = redisTemplate.opsForValue().get("auth:logout:" + userId);
            if (logoutTimestamp != null
                    && jwtUtil.extractIssuedAt(refreshToken).getTime() <= Long
                            .parseLong(logoutTimestamp)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
            }

            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

            String newAccessToken = generateAccessToken(userId, user.getRole());
            return new RefreshResponseDTO(newAccessToken);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
    }

    public LogoutResponseDTO logout(String refreshToken) {
        try {
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
            }

            var userId = jwtUtil.extractUserId(refreshToken);
            String tokenId = jwtUtil.extractTokenId(refreshToken);

            userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

            Date refreshTokenExpiry = jwtUtil.extractExpiration(refreshToken);
            long ttlMillis = refreshTokenExpiry.getTime() - System.currentTimeMillis();

            if (ttlMillis > 0) {
                redisTemplate.opsForValue().set(
                        refreshTokenBlacklistKey(tokenId),
                        userId.toString(),
                        Duration.ofMillis(ttlMillis));
            }

            redisTemplate.opsForValue().set(
                    "auth:logout:" + userId,
                    String.valueOf(System.currentTimeMillis()),
                    Duration.ofMillis(Math.max(ttlMillis, 1)));

            return new LogoutResponseDTO("Successfully logged out");

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
    }

    private String refreshTokenBlacklistKey(String tokenId) {
        return "auth:blacklist:refresh:" + tokenId;
    }

    public AuthTokensDTO register(CreateUserRequestDTO request) {

        Users user = userService.createUser(request);

        String accessToken = generateAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        return new AuthTokensDTO(accessToken, refreshToken, normalizeRole(user.getRole()));
    }

    private String generateAccessToken(java.util.UUID userId, String role) {
        return jwtUtil.generateAccessToken(userId, normalizeRole(role));
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "USER";
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        if (normalizedRole.startsWith("ROLE_")) {
            return normalizedRole.substring("ROLE_".length());
        }

        return normalizedRole;
    }
}
