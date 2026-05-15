package com.sneaky.sneaky.services.analytics;

import com.sneaky.sneaky.dto.analytics.UserActivityEventDTO;

public interface ActivityEventPublisher {
    void publish(UserActivityEventDTO event);
}
