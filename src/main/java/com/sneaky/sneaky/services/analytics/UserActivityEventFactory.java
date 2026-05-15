package com.sneaky.sneaky.services.analytics;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sneaky.sneaky.dto.analytics.UserActivityEventDTO;
import com.sneaky.sneaky.dto.analytics.UserActivityEventType;

@Component
public class UserActivityEventFactory {
    public UserActivityEventDTO create(
            UserActivityEventType eventType,
            UUID userId,
            UUID productId,
            Integer quantity) {
        return UserActivityEventDTO.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .occurredAt(Instant.now())
                .build();
    }
}
