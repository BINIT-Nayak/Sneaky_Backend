package com.sneaky.sneaky.services.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class ProductRecommendationCacheTest {
    private final StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
    private final ListOperations<String, String> listOperations = org.mockito.Mockito.mock(ListOperations.class);
    private final ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
    private final ProductRecommendationCache cache = new ProductRecommendationCache(redisTemplate);

    @Test
    void getReturnsCachedProductIdsForUserKey() {
        UUID userId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(listOperations.range("recommendations:user:" + userId, 0, 29))
                .thenReturn(List.of(firstProductId.toString(), secondProductId.toString()));
        when(valueOperations.get("recommendations:user:" + userId + ":personalized")).thenReturn("true");

        Optional<ProductRecommendationCache.CachedRecommendationIds> cached = cache.get(userId, 30);

        assertThat(cached).isPresent();
        assertThat(cached.get().productIds()).containsExactly(firstProductId, secondProductId);
        assertThat(cached.get().personalized()).isTrue();
    }

    @Test
    void getReturnsEmptyOnMiss() {
        UUID userId = UUID.randomUUID();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("recommendations:user:" + userId, 0, 29)).thenReturn(List.of());

        assertThat(cache.get(userId, 30)).isEmpty();
    }

    @Test
    void putWritesProductIdsAndPersonalizedFlagWithFifteenMinuteTtl() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache.put(userId, List.of(productId), true);

        String key = "recommendations:user:" + userId;
        verify(redisTemplate).delete(key);
        verify(listOperations).rightPushAll(key, List.of(productId.toString()));
        verify(redisTemplate).expire(key, Duration.ofMinutes(15));
        verify(valueOperations).set(key + ":personalized", "true", Duration.ofMinutes(15));
    }

    @Test
    void guestKeyUsesRecommendationsGuest() {
        UUID productId = UUID.randomUUID();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache.put(null, List.of(productId), false);

        verify(listOperations).rightPushAll("recommendations:guest", List.of(productId.toString()));
        verify(valueOperations).set("recommendations:guest:personalized", "false", Duration.ofMinutes(15));
    }
}
