package com.sneaky.sneaky.services;

import java.time.Duration;
import java.util.Date;
import java.util.Locale;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.CreateUserRequestDTO;
import com.sneaky.sneaky.dto.LoginRequestDTO;
import com.sneaky.sneaky.dto.LoginResponseDTO;
import com.sneaky.sneaky.dto.LogoutRequestDTO;
import com.sneaky.sneaky.dto.LogoutResponseDTO;
import com.sneaky.sneaky.dto.RefreshRequestDTO;
import com.sneaky.sneaky.dto.RefreshResponseDTO;
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

    public LoginResponseDTO authenticate(LoginRequestDTO loginRequest) {
        String normalizedEmail = loginRequest.getEmail().trim().toLowerCase(Locale.ROOT);

        Users user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getUserId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        return new LoginResponseDTO(accessToken, refreshToken);
    }

    public RefreshResponseDTO refresh(RefreshRequestDTO refreshRequest) {
        try {
            var userId = jwtUtil.extractUserId(refreshRequest.getRefreshToken());

            if (Boolean.TRUE
                    .equals(redisTemplate.hasKey("auth:blacklist:refresh:" + refreshRequest.getRefreshToken()))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
            }

            String logoutTimestamp = redisTemplate.opsForValue().get("auth:logout:" + userId);
            if (logoutTimestamp != null
                    && jwtUtil.extractIssuedAt(refreshRequest.getRefreshToken()).getTime() <= Long
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

    public LogoutResponseDTO logout(LogoutRequestDTO logoutRequest) {
        try {
            var userId = jwtUtil.extractUserId(logoutRequest.getRefreshToken());

            userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

            Date refreshTokenExpiry = jwtUtil.extractExpiration(logoutRequest.getRefreshToken());
            long ttlMillis = refreshTokenExpiry.getTime() - System.currentTimeMillis();

            if (ttlMillis > 0) {
                redisTemplate.opsForValue().set(
                        "auth:blacklist:refresh:" + logoutRequest.getRefreshToken(),
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

    public LoginResponseDTO register(CreateUserRequestDTO request) {

        Users user = userService.createUser(request);

        String accessToken = jwtUtil.generateAccessToken(user.getUserId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        return new LoginResponseDTO(accessToken, refreshToken);
    }
}
