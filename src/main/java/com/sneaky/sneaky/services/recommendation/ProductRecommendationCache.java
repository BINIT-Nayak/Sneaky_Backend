package com.sneaky.sneaky.services.recommendation;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductRecommendationCache {
    private static final Duration RECOMMENDATION_CACHE_TTL = Duration.ofMinutes(15);
    private static final String GUEST_RECOMMENDATIONS_KEY = "recommendations:guest";
    private static final String PERSONALIZED_SUFFIX = ":personalized";

    private final StringRedisTemplate redisTemplate;

    public Optional<CachedRecommendationIds> get(UUID userId, int limit) {
        String key = recommendationsKey(userId);

        try {
            List<String> cachedIds = redisTemplate.opsForList().range(key, 0, limit - 1);

            if (cachedIds == null || cachedIds.isEmpty()) {
                log.info("Recommendation Redis cache miss. key={}, audience={}", key, audience(userId));
                return Optional.empty();
            }

            List<UUID> productIds = cachedIds.stream()
                    .map(UUID::fromString)
                    .toList();
            boolean personalized = Boolean.parseBoolean(redisTemplate.opsForValue().get(personalizedKey(key)));

            log.info(
                    "Recommendation Redis cache hit. key={}, audience={}, productCount={}, personalized={}",
                    key,
                    audience(userId),
                    productIds.size(),
                    personalized);
            return Optional.of(new CachedRecommendationIds(productIds, personalized));
        } catch (RuntimeException e) {
            log.warn("Recommendation Redis cache read failed. key={}, audience={}", key, audience(userId), e);
            return Optional.empty();
        }
    }

    public void put(UUID userId, List<UUID> productIds, boolean personalized) {
        String key = recommendationsKey(userId);

        try {
            redisTemplate.delete(key);

            if (productIds != null && !productIds.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(
                        key,
                        productIds.stream().map(UUID::toString).toList());
            }

            redisTemplate.expire(key, RECOMMENDATION_CACHE_TTL);
            redisTemplate.opsForValue().set(
                    personalizedKey(key),
                    String.valueOf(personalized),
                    RECOMMENDATION_CACHE_TTL);

            log.info(
                    "Recommendation Redis cache written. key={}, audience={}, productCount={}, ttlSeconds={}, personalized={}",
                    key,
                    audience(userId),
                    productIds == null ? 0 : productIds.size(),
                    RECOMMENDATION_CACHE_TTL.toSeconds(),
                    personalized);
        } catch (RuntimeException e) {
            log.warn("Recommendation Redis cache write failed. key={}, audience={}", key, audience(userId), e);
        }
    }

    public void invalidate(UUID userId) {
        String key = recommendationsKey(userId);

        try {
            redisTemplate.delete(List.of(key, personalizedKey(key)));
            log.info("Recommendation Redis cache invalidated. key={}, audience={}", key, audience(userId));
        } catch (RuntimeException e) {
            log.warn("Recommendation Redis cache invalidation failed. key={}, audience={}", key, audience(userId), e);
        }
    }

    static String recommendationsKey(UUID userId) {
        return userId == null
                ? GUEST_RECOMMENDATIONS_KEY
                : "recommendations:user:" + userId;
    }

    private static String personalizedKey(String recommendationsKey) {
        return recommendationsKey + PERSONALIZED_SUFFIX;
    }

    private static String audience(UUID userId) {
        return userId == null ? "guest" : "user";
    }

    public record CachedRecommendationIds(List<UUID> productIds, boolean personalized) {
    }
}
