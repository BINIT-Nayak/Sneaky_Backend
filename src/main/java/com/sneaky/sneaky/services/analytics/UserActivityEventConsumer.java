package com.sneaky.sneaky.services.analytics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.sneaky.sneaky.dto.analytics.UserActivityEventDTO;
import com.sneaky.sneaky.services.ProductRecommendationService;
import com.sneaky.sneaky.services.recommendation.ProductRecommendationCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class UserActivityEventConsumer {
    private final ProductAnalyticsService productAnalyticsService;
    private final UserPreferenceProfileService userPreferenceProfileService;
    private final ProductRecommendationCache recommendationCache;
    private final ProductRecommendationService productRecommendationService;

    @KafkaListener(topics = "${app.kafka.topics.user-activity}")
    public void consume(UserActivityEventDTO event) {
        userPreferenceProfileService.applyEvent(event);
        productAnalyticsService.record(event);
        invalidateUserRecommendations(event);
        refreshUserRecommendations(event);
    }

    private void invalidateUserRecommendations(UserActivityEventDTO event) {
        if (event == null || event.getUserId() == null) {
            return;
        }

        recommendationCache.invalidate(event.getUserId());
    }

    private void refreshUserRecommendations(UserActivityEventDTO event) {
        if (event == null || event.getUserId() == null) {
            return;
        }

        try {
            productRecommendationService.refreshRecommendedProducts(event.getUserId());
        } catch (RuntimeException ex) {
            log.warn("Failed to refresh recommendations for user {}", event.getUserId(), ex);
        }
    }
}
