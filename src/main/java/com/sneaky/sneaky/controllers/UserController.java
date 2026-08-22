package com.sneaky.sneaky.controllers;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import com.sneaky.sneaky.dto.auth.AuthTokensDTO;
import com.sneaky.sneaky.dto.auth.LoginRequestDTO;
import com.sneaky.sneaky.dto.auth.LoginResponseDTO;
import com.sneaky.sneaky.dto.user.*;
import com.sneaky.sneaky.security.CurrentUser;
import com.sneaky.sneaky.services.AuthService;
import com.sneaky.sneaky.services.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final String REFRESH_COOKIE_NAME = "sneaky_refresh_token";
    private static final Duration REFRESH_COOKIE_MAX_AGE = Duration.ofDays(7);

    private final UserService userService;
    private final AuthService authService;
    private final CurrentUser currentUser;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;

    public UserController(
            UserService userService,
            AuthService authService,
            CurrentUser currentUser,
            @Value("${app.auth.refresh-cookie.secure:false}") boolean refreshCookieSecure,
            @Value("${app.auth.refresh-cookie.same-site:Lax}") String refreshCookieSameSite) {
        this.userService = userService;
        this.authService = authService;
        this.currentUser = currentUser;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
    }

    @GetMapping
    public List<UserDTO> getUsers() {
        return userService.getAllUsers();
    }

    @PostMapping
    public ResponseEntity<LoginResponseDTO> createUser(@Valid @RequestBody CreateUserRequestDTO request) {
        userService.createUser(request);

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail(request.getEmail());
        loginRequest.setPassword(request.getPassword());

        AuthTokensDTO tokens = authService.authenticate(loginRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.getRefreshToken()).toString())
                .body(tokens.toLoginResponse());
    }

    @GetMapping("/me")
    public UserDTO getCurrentUser() {
        return userService.getUserById(currentUser.getUserId());
    }

    @GetMapping("/me/profile-summary")
    public ProfileSummaryDTO getProfileSummary() {
        return userService.getProfileSummary(currentUser.getUserId());
    }

    @PutMapping("/me")
    public UserDTO updateCurrentUser(@Valid @RequestBody UpdateUserRequestDTO request) {
        return userService.updateUserById(currentUser.getUserId(), request);
    }

    @PatchMapping("/me")
    public UserDTO patchCurrentUser(@Valid @RequestBody UpdateUserRequestDTO request) {
        return userService.patchUserById(currentUser.getUserId(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCurrentUser() {
        userService.deleteUserById(currentUser.getUserId());
    }

    private ResponseCookie refreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/api/auth")
                .maxAge(REFRESH_COOKIE_MAX_AGE)
                .build();
    }
}
