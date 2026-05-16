package com.sneaky.sneaky.services.analytics;

import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.sneaky.sneaky.dto.analytics.ProductAnalyticsDTO;
import com.sneaky.sneaky.dto.analytics.UserActivityEventDTO;
import com.sneaky.sneaky.dto.analytics.UserActivityEventType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductAnalyticsService {
    private static final int RECENTLY_VIEWED_LIMIT = 20;

    private final StringRedisTemplate redisTemplate;

    public void record(UserActivityEventDTO event) {
        if (event == null || event.getEventType() == null || event.getProductId() == null) {
            return;
        }

        UUID productId = event.getProductId();

        if (event.getEventType() == UserActivityEventType.PRODUCT_VIEWED) {
            redisTemplate.opsForValue().increment(productMetricKey(productId, "views"));
            redisTemplate.opsForZSet().incrementScore("analytics:products:most-viewed", productId.toString(), 1);
            recordRecentlyViewed(event);
            return;
        }

        if (event.getEventType() == UserActivityEventType.CART_ADDED) {
            redisTemplate.opsForValue().increment(productMetricKey(productId, "cart-adds"));
            return;
        }

        if (event.getEventType() == UserActivityEventType.WISHLIST_ADDED) {
            redisTemplate.opsForValue().increment(productMetricKey(productId, "wishlist-adds"));
        }
    }

    public ProductAnalyticsDTO getProductAnalytics(UUID productId) {
        return new ProductAnalyticsDTO(
                productId,
                readLong(productMetricKey(productId, "views")),
                readLong(productMetricKey(productId, "cart-adds")),
                readLong(productMetricKey(productId, "wishlist-adds")));
    }

    public List<UUID> getRecentlyViewedProductIds(UUID userId) {
        List<String> productIds = redisTemplate.opsForList().range(recentlyViewedKey(userId), 0, RECENTLY_VIEWED_LIMIT - 1);

        if (productIds == null) {
            return List.of();
        }

        return productIds.stream()
                .map(UUID::fromString)
                .toList();
    }

    public List<UUID> getMostViewedProductIds(int limit) {
        if (limit <= 0) {
            return List.of();
        }

        var productIds = redisTemplate.opsForZSet().reverseRange("analytics:products:most-viewed", 0, limit - 1);

        if (productIds == null) {
            return List.of();
        }

        return productIds.stream()
                .map(UUID::fromString)
                .toList();
    }

    private void recordRecentlyViewed(UserActivityEventDTO event) {
        if (event.getUserId() == null) {
            return;
        }

        String key = recentlyViewedKey(event.getUserId());
        String productId = event.getProductId().toString();
        redisTemplate.opsForList().remove(key, 0, productId);
        redisTemplate.opsForList().leftPush(key, productId);
        redisTemplate.opsForList().trim(key, 0, RECENTLY_VIEWED_LIMIT - 1);
    }

    private long readLong(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0 : Long.parseLong(value);
    }

    private static String productMetricKey(UUID productId, String metric) {
        return "analytics:product:" + productId + ":" + metric;
    }

    private static String recentlyViewedKey(UUID userId) {
        return "analytics:user:" + userId + ":recently-viewed";
    }
}
