package com.sneaky.sneaky.services.analytics;

import java.time.Duration;
import java.time.LocalDateTime;

public final class PreferenceDecay {
    private PreferenceDecay() {
    }

    public static double factor(LocalDateTime updatedAt) {
        if (updatedAt == null) {
            return 1.0;
        }

        long ageDays = Duration.between(updatedAt, LocalDateTime.now()).toDays();

        if (ageDays < 7) {
            return 1.0;
        }

        if (ageDays < 30) {
            return 0.8;
        }

        if (ageDays < 90) {
            return 0.6;
        }

        return 0.3;
    }
}
