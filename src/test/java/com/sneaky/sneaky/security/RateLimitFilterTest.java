package com.sneaky.sneaky.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {
    private final StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
    private final RateLimitFilter filter = new RateLimitFilter(
            redisTemplate,
            3,
            300,
            3,
            300,
            30,
            60,
            240,
            60,
            60,
            60,
            60,
            60);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void allowsLoginRequestsWithinLimit() throws Exception {
        MockHttpServletRequest request = loginRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(valueOperations.increment("rate-limit:login:ip:127.0.0.1")).thenReturn(3L);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
    }

    @Test
    void rejectsLoginRequestsOverLimit() throws Exception {
        MockHttpServletRequest request = loginRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(valueOperations.increment("rate-limit:login:ip:127.0.0.1")).thenReturn(4L);
        when(redisTemplate.getExpire("rate-limit:login:ip:127.0.0.1", TimeUnit.SECONDS)).thenReturn(240L);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("240");
        assertThat(response.getContentAsString()).contains("Try again after 5 minutes");
    }

    @Test
    void setsExpiryWhenWindowStarts() throws Exception {
        MockHttpServletRequest request = loginRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(valueOperations.increment("rate-limit:login:ip:127.0.0.1")).thenReturn(1L);

        filter.doFilter(request, response, new MockFilterChain());

        verify(redisTemplate).expire("rate-limit:login:ip:127.0.0.1", Duration.ofSeconds(300));
    }

    @Test
    void rejectsRegistrationRequestsOverLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(valueOperations.increment("rate-limit:register:ip:127.0.0.1")).thenReturn(4L);
        when(redisTemplate.getExpire("rate-limit:register:ip:127.0.0.1", TimeUnit.SECONDS)).thenReturn(300L);

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Too many sign up attempts");
    }

    @Test
    void rateLimitsPublicProductReadsByIp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(valueOperations.increment("rate-limit:public-read:ip:127.0.0.1")).thenReturn(241L);
        when(redisTemplate.getExpire("rate-limit:public-read:ip:127.0.0.1", TimeUnit.SECONDS)).thenReturn(40L);

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("40");
    }

    @Test
    void rateLimitsCartMutationsByAuthenticatedUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-123", null, java.util.List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/cart");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(valueOperations.increment("rate-limit:user-mutation:user:user-123")).thenReturn(61L);
        when(redisTemplate.getExpire("rate-limit:user-mutation:user:user-123", TimeUnit.SECONDS)).thenReturn(12L);

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Too many actions");
    }

    @Test
    void ignoresNonLoginRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(redisTemplate, never()).opsForValue();
    }

    private static MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
