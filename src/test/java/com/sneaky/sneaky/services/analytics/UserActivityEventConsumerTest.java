package com.sneaky.sneaky.services.analytics;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.inOrder;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sneaky.sneaky.dto.analytics.UserActivityEventDTO;
import com.sneaky.sneaky.dto.analytics.UserActivityEventType;
import com.sneaky.sneaky.services.ProductRecommendationService;
import com.sneaky.sneaky.services.recommendation.ProductRecommendationCache;

class UserActivityEventConsumerTest {

    private final ProductAnalyticsService productAnalyticsService = org.mockito.Mockito.mock(ProductAnalyticsService.class);
    private final UserPreferenceProfileService userPreferenceProfileService =
            org.mockito.Mockito.mock(UserPreferenceProfileService.class);
    private final ProductRecommendationCache recommendationCache =
            org.mockito.Mockito.mock(ProductRecommendationCache.class);
    private final ProductRecommendationService productRecommendationService =
            org.mockito.Mockito.mock(ProductRecommendationService.class);
    private final UserActivityEventConsumer consumer =
            new UserActivityEventConsumer(
                    productAnalyticsService,
                    userPreferenceProfileService,
                    recommendationCache,
                    productRecommendationService);

    @Test
    void consumeRecordsAnalyticsAndRefreshesUserRecommendations() {
        UUID userId = UUID.randomUUID();
        UserActivityEventDTO event = UserActivityEventDTO.builder()
                .eventType(UserActivityEventType.SKIP)
                .userId(userId)
                .productId(UUID.randomUUID())
                .build();

        consumer.consume(event);

        verify(userPreferenceProfileService).applyEvent(event);
        verify(productAnalyticsService).record(event);
        verify(recommendationCache).invalidate(userId);
        verify(productRecommendationService).refreshRecommendedProducts(userId);

        var inOrder = inOrder(userPreferenceProfileService, productAnalyticsService, recommendationCache, productRecommendationService);
        inOrder.verify(userPreferenceProfileService).applyEvent(event);
        inOrder.verify(productAnalyticsService).record(event);
        inOrder.verify(recommendationCache).invalidate(userId);
        inOrder.verify(productRecommendationService).refreshRecommendedProducts(userId);
    }
}
