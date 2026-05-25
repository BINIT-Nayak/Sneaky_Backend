package com.sneaky.sneaky.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.repository.UsersRepository;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;

class JwtFilterTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final UsersRepository usersRepository = mock(UsersRepository.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final JwtFilter jwtFilter = new JwtFilter(jwtUtil, redisTemplate, usersRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesBearerTokenWhenTokenWasNotLoggedOut() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        Users user = new Users();
        user.setUserId(USER_ID);
        user.setRole("ADMIN");

        when(jwtUtil.isAccessToken("access-token")).thenReturn(true);
        when(jwtUtil.extractUserId("access-token")).thenReturn(USER_ID);
        when(usersRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:logout:" + USER_ID)).thenReturn(null);

        jwtFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(USER_ID.toString());
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void normalizesPersistedSpringRolePrefix() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtUtil.isAccessToken("access-token")).thenReturn(true);
        when(jwtUtil.extractUserId("access-token")).thenReturn(USER_ID);
        when(usersRepository.findById(USER_ID)).thenReturn(Optional.of(user("ROLE_ADMIN", false)));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:logout:" + USER_ID)).thenReturn(null);

        jwtFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void skipsAuthenticationWhenTokenWasIssuedBeforeLogout() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        Users user = new Users();
        user.setUserId(USER_ID);

        when(jwtUtil.isAccessToken("access-token")).thenReturn(true);
        when(jwtUtil.extractUserId("access-token")).thenReturn(USER_ID);
        when(usersRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:logout:" + USER_ID)).thenReturn("2000");
        when(jwtUtil.extractIssuedAt("access-token")).thenReturn(new Date(1000));

        jwtFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void skipsAuthenticationWhenTokenUserNoLongerExists() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtUtil.isAccessToken("access-token")).thenReturn(true);
        when(jwtUtil.extractUserId("access-token")).thenReturn(USER_ID);
        when(usersRepository.findById(USER_ID)).thenReturn(Optional.empty());

        jwtFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void rejectsMalformedBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtUtil.isAccessToken("bad-token")).thenThrow(new JwtException("bad token"));

        jwtFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void rejectsRefreshTokenUsedAsBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtUtil.isAccessToken("refresh-token")).thenReturn(false);

        jwtFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(request, response);
    }

    private Users user(String role, boolean banned) {
        Users user = new Users();
        user.setUserId(USER_ID);
        user.setRole(role);
        user.setIsBanned(banned);
        return user;
    }
}
