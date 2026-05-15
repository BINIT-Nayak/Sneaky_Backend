package com.sneaky.sneaky.services.analytics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.sneaky.sneaky.dto.analytics.UserActivityEventDTO;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpActivityEventPublisher implements ActivityEventPublisher {
    @Override
    public void publish(UserActivityEventDTO event) {
        // Kafka is opt-in for local development.
    }
}
