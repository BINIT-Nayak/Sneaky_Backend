package com.sneaky.sneaky.dto.analytics;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityEventDTO {
    private UUID eventId;
    private UserActivityEventType eventType;
    private UUID userId;
    private UUID productId;
    private Integer quantity;
    private Instant occurredAt;
    private Map<String, Object> metadata;
}
