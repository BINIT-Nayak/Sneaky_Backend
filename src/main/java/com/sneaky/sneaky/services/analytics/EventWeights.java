package com.sneaky.sneaky.services.analytics;

import com.sneaky.sneaky.dto.analytics.UserActivityEventType;

public final class EventWeights {
    private EventWeights() {
    }

    public static double weight(UserActivityEventType eventType) {
        return switch (eventType) {
            case IMPRESSION -> 0.0;
            case VIEW -> 0.5;
            case CLICK -> 1.0;
            case SKIP -> -1.0;
            case WISHLIST -> 3.0;
            case CART -> 4.0;
            case PURCHASE -> 5.0;
        };
    }

    public static double normalizedDelta(UserActivityEventType eventType) {
        return weight(eventType) / 5.0;
    }
}
