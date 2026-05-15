package com.sneaky.sneaky.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sneaky.sneaky.dto.auth.AuthTokensDTO;
import com.sneaky.sneaky.dto.auth.LoginRequestDTO;
import com.sneaky.sneaky.dto.auth.LogoutResponseDTO;
import com.sneaky.sneaky.dto.auth.RefreshResponseDTO;
import com.sneaky.sneaky.services.AuthService;

import jakarta.servlet.http.Cookie;

class AuthControllerTest {

        private final AuthService authService = org.mockito.Mockito.mock(AuthService.class);
        private final ObjectMapper objectMapper = new ObjectMapper();
        private static final String REFRESH_COOKIE_NAME = "sneaky_refresh_token";
        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
                validator.afterPropertiesSet();
                mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, false, "Lax"))
                                .setValidator(validator)
                                .build();
        }

        @Test
        void loginReturnsAccessTokenAndRefreshCookieForValidRequest() throws Exception {
                LoginRequestDTO request = new LoginRequestDTO();
                request.setEmail("dev@example.com");
                request.setPassword("secret123");

                when(authService.authenticate(any(LoginRequestDTO.class)))
                                .thenReturn(new AuthTokensDTO("access", "refresh"));

                mockMvc.perform(post("/api/auth/login")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("access"))
                                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                                .andExpect(cookie().httpOnly(REFRESH_COOKIE_NAME, true))
                                .andExpect(cookie().value(REFRESH_COOKIE_NAME, "refresh"));

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
        void refreshAndLogoutDelegateCookieRefreshToken() throws Exception {
                when(authService.refresh(any(String.class))).thenReturn(new RefreshResponseDTO("access"));
                when(authService.logout(any(String.class)))
                                .thenReturn(new LogoutResponseDTO("Successfully logged out"));

                mockMvc.perform(post("/api/auth/refresh")
                                .cookie(new Cookie(REFRESH_COOKIE_NAME, "refresh-token")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("access"));

                mockMvc.perform(post("/api/auth/logout")
                                .cookie(new Cookie(REFRESH_COOKIE_NAME, "refresh-token")))
                                .andExpect(status().isOk())
                                .andExpect(cookie().maxAge(REFRESH_COOKIE_NAME, 0))
                                .andExpect(jsonPath("$.message").value("Successfully logged out"));

                ArgumentCaptor<String> refreshTokenCaptor = ArgumentCaptor.forClass(String.class);
                verify(authService).refresh(refreshTokenCaptor.capture());
                assertThat(refreshTokenCaptor.getValue()).isEqualTo("refresh-token");
                verify(authService).logout("refresh-token");
        }
}
