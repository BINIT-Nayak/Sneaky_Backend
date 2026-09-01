package com.sneaky.sneaky.services.analytics;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
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
        return create(eventType, userId, productId, quantity, Map.of());
    }

    public UserActivityEventDTO create(
            UserActivityEventType eventType,
            UUID userId,
            UUID productId,
            Integer quantity,
            Map<String, Object> metadata) {
        return UserActivityEventDTO.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .occurredAt(Instant.now())
                .metadata(cleanMetadata(metadata))
                .build();
    }

    private static Map<String, Object> cleanMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> clean = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && value != null) {
                clean.put(key, value);
            }
        });
        return Map.copyOf(clean);
    }
}
