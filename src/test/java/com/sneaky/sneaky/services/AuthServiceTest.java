package com.sneaky.sneaky.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.auth.LoginRequestDTO;
import com.sneaky.sneaky.dto.auth.LoginResponseDTO;
import com.sneaky.sneaky.dto.auth.LogoutRequestDTO;
import com.sneaky.sneaky.dto.auth.LogoutResponseDTO;
import com.sneaky.sneaky.dto.auth.RefreshRequestDTO;
import com.sneaky.sneaky.dto.auth.RefreshResponseDTO;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.repository.UsersRepository;
import com.sneaky.sneaky.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private UsersRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    @Test
    void authenticateReturnsAccessAndRefreshTokensWhenCredentialsMatch() {
        Users user = user("dev@example.com", "encoded");
        LoginRequestDTO request = loginRequest("Dev@Example.com", "plain");

        when(userRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain", "encoded")).thenReturn(true);
        when(jwtUtil.generateAccessToken(USER_ID)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(USER_ID)).thenReturn("refresh-token");

        LoginResponseDTO response = authService.authenticate(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void authenticateRejectsInvalidPassword() {
        Users user = user("dev@example.com", "encoded");

        when(userRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(loginRequest("dev@example.com", "wrong")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(jwtUtil, never()).generateAccessToken(any(UUID.class));
    }

    @Test
    void refreshRejectsBlacklistedRefreshToken() {
        RefreshRequestDTO request = refreshRequest("refresh-token");

        when(jwtUtil.extractUserId("refresh-token")).thenReturn(USER_ID);
        when(redisTemplate.hasKey("auth:blacklist:refresh:refresh-token")).thenReturn(true);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshReturnsNewAccessTokenForValidRefreshToken() {
        RefreshRequestDTO request = refreshRequest("refresh-token");

        when(jwtUtil.extractUserId("refresh-token")).thenReturn(USER_ID);
        when(redisTemplate.hasKey("auth:blacklist:refresh:refresh-token")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:logout:" + USER_ID)).thenReturn(null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user("dev@example.com", "encoded")));
        when(jwtUtil.generateAccessToken(USER_ID)).thenReturn("new-access-token");

        RefreshResponseDTO response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
    }

    @Test
    void logoutBlacklistsRefreshTokenAndStoresLogoutTimestamp() {
        LogoutRequestDTO request = new LogoutRequestDTO();
        request.setRefreshToken("refresh-token");

        when(jwtUtil.extractUserId("refresh-token")).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user("dev@example.com", "encoded")));
        when(jwtUtil.extractExpiration("refresh-token")).thenReturn(new Date(System.currentTimeMillis() + 60_000));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        LogoutResponseDTO response = authService.logout(request);

        assertThat(response.getMessage()).isEqualTo("Successfully logged out");
        verify(valueOperations).set(
                eq("auth:blacklist:refresh:refresh-token"),
                eq(USER_ID.toString()),
                any(Duration.class));
        verify(valueOperations).set(
                eq("auth:logout:" + USER_ID),
                anyString(),
                any(Duration.class));
    }

    private static Users user(String email, String password) {
        Users user = new Users();
        user.setUserId(USER_ID);
        user.setEmail(email);
        user.setPassword(password);
        return user;
    }

    private static LoginRequestDTO loginRequest(String email, String password) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private static RefreshRequestDTO refreshRequest(String token) {
        RefreshRequestDTO request = new RefreshRequestDTO();
        request.setRefreshToken(token);
        return request;
    }
}
