package com.sneaky.sneaky.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final RateLimitPolicy loginPolicy;
    private final RateLimitPolicy registerPolicy;
    private final RateLimitPolicy refreshPolicy;
    private final RateLimitPolicy publicReadPolicy;
    private final RateLimitPolicy userMutationPolicy;
    private final RateLimitPolicy analyticsPolicy;

    public RateLimitFilter(
            StringRedisTemplate redisTemplate,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.login.limit:3}") int loginLimit,
            @Value("${app.rate-limit.login.window-seconds:300}") long loginWindowSeconds,
            @Value("${app.rate-limit.register.limit:3}") int registerLimit,
            @Value("${app.rate-limit.register.window-seconds:300}") long registerWindowSeconds,
            @Value("${app.rate-limit.refresh.limit:30}") int refreshLimit,
            @Value("${app.rate-limit.refresh.window-seconds:60}") long refreshWindowSeconds,
            @Value("${app.rate-limit.public-read.limit:240}") int publicReadLimit,
            @Value("${app.rate-limit.public-read.window-seconds:60}") long publicReadWindowSeconds,
            @Value("${app.rate-limit.user-mutation.limit:60}") int userMutationLimit,
            @Value("${app.rate-limit.user-mutation.window-seconds:60}") long userMutationWindowSeconds,
            @Value("${app.rate-limit.analytics.limit:60}") int analyticsLimit,
            @Value("${app.rate-limit.analytics.window-seconds:60}") long analyticsWindowSeconds) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.loginPolicy = new RateLimitPolicy(
                "login",
                loginLimit,
                Duration.ofSeconds(loginWindowSeconds),
                request -> isPost(request, "/api/auth/login"),
                "Too many login attempts. Try again after 5 minutes.");
        this.registerPolicy = new RateLimitPolicy(
                "register",
                registerLimit,
                Duration.ofSeconds(registerWindowSeconds),
                request -> isPost(request, "/api/auth/register") || isPost(request, "/api/users"),
                "Too many sign up attempts. Try again after 5 minutes.");
        this.refreshPolicy = new RateLimitPolicy(
                "refresh",
                refreshLimit,
                Duration.ofSeconds(refreshWindowSeconds),
                request -> isPost(request, "/api/auth/refresh"),
                "Too many session refresh attempts. Please wait a moment and try again.");
        this.publicReadPolicy = new RateLimitPolicy(
                "public-read",
                publicReadLimit,
                Duration.ofSeconds(publicReadWindowSeconds),
                request -> HttpMethod.GET.matches(request.getMethod())
                        && (request.getRequestURI().startsWith("/api/products")
                                || request.getRequestURI().startsWith("/api/brands")),
                "Too many requests. Please wait a moment and try again.");
        this.userMutationPolicy = new RateLimitPolicy(
                "user-mutation",
                userMutationLimit,
                Duration.ofSeconds(userMutationWindowSeconds),
                request -> isMutatingMethod(request)
                        && (request.getRequestURI().startsWith("/api/cart")
                                || request.getRequestURI().startsWith("/api/wishlist")
                                || request.getRequestURI().startsWith("/api/users/me")),
                "Too many actions. Please wait a moment and try again.");
        this.analyticsPolicy = new RateLimitPolicy(
                "analytics",
                analyticsLimit,
                Duration.ofSeconds(analyticsWindowSeconds),
                request -> request.getRequestURI().startsWith("/api/product-analytics"),
                "Too many analytics requests. Please wait a moment and try again.");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || resolvePolicy(request) == null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        RateLimitPolicy policy = resolvePolicy(request);

        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = rateLimitKey(request, policy);

        try {
            Long count = redisTemplate.opsForValue().increment(key);

            if (count != null && count == 1) {
                redisTemplate.expire(key, policy.window());
            }

            if (count != null && count > policy.limit()) {
                writeRateLimitedResponse(response, key, policy);
                return;
            }

            if (count != null) {
                response.setHeader("X-RateLimit-Limit", String.valueOf(policy.limit()));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, policy.limit() - count)));
            }
        } catch (RuntimeException ex) {
            log.warn("Rate limiting skipped because Redis is unavailable", ex);
        }

        filterChain.doFilter(request, response);
    }

    private void writeRateLimitedResponse(
            HttpServletResponse response,
            String key,
            RateLimitPolicy policy) throws IOException {
        long retryAfterSeconds = retryAfterSeconds(key, policy);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setHeader("X-RateLimit-Limit", String.valueOf(policy.limit()));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setContentType("application/json");
        response.getWriter().write("""
                {"status":429,"error":"Too Many Requests","message":"%s"}
                """.formatted(policy.message()));
    }

    private long retryAfterSeconds(String key, RateLimitPolicy policy) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl == null || ttl < 1 ? policy.window().toSeconds() : ttl;
    }

    private RateLimitPolicy resolvePolicy(HttpServletRequest request) {
        if (loginPolicy.matches(request)) {
            return loginPolicy;
        }
        if (registerPolicy.matches(request)) {
            return registerPolicy;
        }
        if (refreshPolicy.matches(request)) {
            return refreshPolicy;
        }
        if (userMutationPolicy.matches(request)) {
            return userMutationPolicy;
        }
        if (analyticsPolicy.matches(request)) {
            return analyticsPolicy;
        }
        if (publicReadPolicy.matches(request)) {
            return publicReadPolicy;
        }

        return null;
    }

    private String rateLimitKey(HttpServletRequest request, RateLimitPolicy policy) {
        return "rate-limit:" + policy.name() + ":" + principalOrIp(request);
    }

    private String principalOrIp(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
            return "user:" + authentication.getName();
        }

        return "ip:" + clientIp(request);
    }

    private boolean isPost(HttpServletRequest request, String path) {
        return HttpMethod.POST.matches(request.getMethod()) && path.equals(request.getRequestURI());
    }

    private boolean isMutatingMethod(HttpServletRequest request) {
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        return method.equals("POST") || method.equals("PUT") || method.equals("PATCH") || method.equals("DELETE");
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private record RateLimitPolicy(
            String name,
            int limit,
            Duration window,
            Predicate<HttpServletRequest> matcher,
            String message) {

        boolean matches(HttpServletRequest request) {
            return matcher.test(request);
        }
    }
}
