package com.sneaky.sneaky.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sneaky.sneaky.dto.auth.LoginRequestDTO;
import com.sneaky.sneaky.dto.auth.LoginResponseDTO;
import com.sneaky.sneaky.dto.auth.LogoutRequestDTO;
import com.sneaky.sneaky.dto.auth.LogoutResponseDTO;
import com.sneaky.sneaky.dto.auth.RefreshRequestDTO;
import com.sneaky.sneaky.dto.auth.RefreshResponseDTO;
import com.sneaky.sneaky.services.AuthService;

class AuthControllerTest {

    private final AuthService authService = org.mockito.Mockito.mock(AuthService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setValidator(validator)
                .build();
    }

    @Test
    void loginReturnsTokensForValidRequest() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("dev@example.com");
        request.setPassword("secret123");

        when(authService.authenticate(any(LoginRequestDTO.class)))
                .thenReturn(new LoginResponseDTO("access", "refresh"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));

        verify(authService).authenticate(any(LoginRequestDTO.class));
    }

    @Test
    void loginRejectsInvalidEmailBeforeServiceCall() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("bad-email");
        request.setPassword("secret123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void refreshAndLogoutDelegateValidRequests() throws Exception {
        RefreshRequestDTO refreshRequest = new RefreshRequestDTO();
        refreshRequest.setRefreshToken("refresh-token");
        LogoutRequestDTO logoutRequest = new LogoutRequestDTO();
        logoutRequest.setRefreshToken("refresh-token");

        when(authService.refresh(any(RefreshRequestDTO.class))).thenReturn(new RefreshResponseDTO("access"));
        when(authService.logout(any(LogoutRequestDTO.class))).thenReturn(new LogoutResponseDTO("Successfully logged out"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully logged out"));
    }
}
