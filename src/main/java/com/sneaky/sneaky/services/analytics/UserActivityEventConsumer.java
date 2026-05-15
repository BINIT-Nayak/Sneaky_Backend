package com.sneaky.sneaky.services.analytics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.sneaky.sneaky.dto.analytics.UserActivityEventDTO;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class UserActivityEventConsumer {
    private final ProductAnalyticsService productAnalyticsService;

    @KafkaListener(topics = "${app.kafka.topics.user-activity}")
    public void consume(UserActivityEventDTO event) {
        productAnalyticsService.record(event);
    }
}
