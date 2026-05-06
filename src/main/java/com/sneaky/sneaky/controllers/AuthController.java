package com.sneaky.sneaky.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sneaky.sneaky.dto.CreateUserRequestDTO;
import com.sneaky.sneaky.dto.LoginRequestDTO;
import com.sneaky.sneaky.dto.LoginResponseDTO;
import com.sneaky.sneaky.dto.LogoutRequestDTO;
import com.sneaky.sneaky.dto.LogoutResponseDTO;
import com.sneaky.sneaky.dto.RefreshRequestDTO;
import com.sneaky.sneaky.dto.RefreshResponseDTO;
import com.sneaky.sneaky.services.AuthService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        return authService.authenticate(loginRequest);
    }

    @PostMapping("/refresh")
    public RefreshResponseDTO refresh(@Valid @RequestBody RefreshRequestDTO refreshRequest) {
        return authService.refresh(refreshRequest);
    }

    @PostMapping("/logout")
    public LogoutResponseDTO logout(@Valid @RequestBody LogoutRequestDTO logoutRequest) {
        return authService.logout(logoutRequest);
    }

    @PostMapping("/register")
    public LoginResponseDTO register(@Valid @RequestBody CreateUserRequestDTO request) {
        return authService.register(request);
    }
}