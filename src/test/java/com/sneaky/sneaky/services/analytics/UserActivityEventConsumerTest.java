package com.sneaky.sneaky.services.analytics;

import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sneaky.sneaky.dto.analytics.UserActivityEventDTO;
import com.sneaky.sneaky.dto.analytics.UserActivityEventType;
import com.sneaky.sneaky.services.ProductRecommendationService;

class UserActivityEventConsumerTest {

    private final ProductAnalyticsService productAnalyticsService = org.mockito.Mockito.mock(ProductAnalyticsService.class);
    private final ProductRecommendationService productRecommendationService =
            org.mockito.Mockito.mock(ProductRecommendationService.class);
    private final UserActivityEventConsumer consumer =
            new UserActivityEventConsumer(productAnalyticsService, productRecommendationService);

    @Test
    void consumeRecordsAnalyticsAndRefreshesUserRecommendations() {
        UUID userId = UUID.randomUUID();
        UserActivityEventDTO event = UserActivityEventDTO.builder()
                .eventType(UserActivityEventType.SKIP)
                .userId(userId)
                .productId(UUID.randomUUID())
                .build();

        consumer.consume(event);

        verify(productAnalyticsService).record(event);
        verify(productRecommendationService).refreshRecommendedProducts(userId);
    }
}
