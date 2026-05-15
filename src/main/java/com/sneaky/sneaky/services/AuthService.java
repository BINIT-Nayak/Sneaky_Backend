package com.sneaky.sneaky.services;

import java.time.Duration;
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
        String normalizedEmail = loginRequest.getEmail().trim().toLowerCase(Locale.ROOT);

        Users user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getUserId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        return new AuthTokensDTO(accessToken, refreshToken);
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

            userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

            String newAccessToken = jwtUtil.generateAccessToken(userId);
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

        String accessToken = jwtUtil.generateAccessToken(user.getUserId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        return new AuthTokensDTO(accessToken, refreshToken);
    }
}
