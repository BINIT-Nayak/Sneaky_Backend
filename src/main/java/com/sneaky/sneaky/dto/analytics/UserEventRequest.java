package com.sneaky.sneaky.dto.analytics;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UserEventRequest(
        @NotNull UUID productId,
        @NotNull UserActivityEventType type,
        String source,
        Integer position,
        Integer quantity,
        Map<String, Object> metadata) {
}
