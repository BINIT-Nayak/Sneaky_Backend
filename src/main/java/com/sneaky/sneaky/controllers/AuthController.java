package com.sneaky.sneaky.controllers;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.auth.AuthTokensDTO;
import com.sneaky.sneaky.dto.auth.LoginRequestDTO;
import com.sneaky.sneaky.dto.auth.LoginResponseDTO;
import com.sneaky.sneaky.dto.auth.LogoutResponseDTO;
import com.sneaky.sneaky.dto.auth.RefreshResponseDTO;
import com.sneaky.sneaky.dto.user.CreateUserRequestDTO;
import com.sneaky.sneaky.services.AuthService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "sneaky_refresh_token";
    private static final Duration REFRESH_COOKIE_MAX_AGE = Duration.ofDays(7);

    private final AuthService authService;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;
    private final String refreshCookieDomain;

    @Autowired
    public AuthController(
            AuthService authService,
            @Value("${app.auth.refresh-cookie.secure:false}") boolean refreshCookieSecure,
            @Value("${app.auth.refresh-cookie.same-site:Lax}") String refreshCookieSameSite,
            @Value("${app.auth.refresh-cookie.domain:}") String refreshCookieDomain) {
        this.authService = authService;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
        this.refreshCookieDomain = refreshCookieDomain == null ? "" : refreshCookieDomain.trim();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        AuthTokensDTO tokens = authService.authenticate(loginRequest);
        return withRefreshCookie(tokens);
    }

    @PostMapping("/refresh")
    public RefreshResponseDTO refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        log.info("Auth refresh requested. refreshCookiePresent={}", refreshToken != null && !refreshToken.isBlank());
        return authService.refresh(requireRefreshToken(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponseDTO> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        LogoutResponseDTO response = authService.logout(requireRefreshToken(refreshToken));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponseDTO> register(@Valid @RequestBody CreateUserRequestDTO request) {
        AuthTokensDTO tokens = authService.register(request);
        return withRefreshCookie(tokens);
    }

    private ResponseEntity<LoginResponseDTO> withRefreshCookie(AuthTokensDTO tokens) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.getRefreshToken()).toString())
                .body(tokens.toLoginResponse());
    }

    private String requireRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is required");
        }
        return refreshToken;
    }

    private ResponseCookie refreshCookie(String refreshToken) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/api/auth")
                .maxAge(REFRESH_COOKIE_MAX_AGE);

        if (!refreshCookieDomain.isBlank()) {
            cookieBuilder.domain(refreshCookieDomain);
        }

        return cookieBuilder.build();
    }

    private ResponseCookie expiredRefreshCookie() {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/api/auth")
                .maxAge(Duration.ZERO);

        if (!refreshCookieDomain.isBlank()) {
            cookieBuilder.domain(refreshCookieDomain);
        }

        return cookieBuilder.build();
    }
}
