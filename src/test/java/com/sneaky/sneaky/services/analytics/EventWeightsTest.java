package com.sneaky.sneaky.services.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sneaky.sneaky.dto.analytics.UserActivityEventType;

class EventWeightsTest {
    @Test
    void weightsAreTransparentBusinessRules() {
        assertThat(EventWeights.weight(UserActivityEventType.IMPRESSION)).isZero();
        assertThat(EventWeights.weight(UserActivityEventType.VIEW)).isEqualTo(0.5);
        assertThat(EventWeights.weight(UserActivityEventType.CLICK)).isEqualTo(1.0);
        assertThat(EventWeights.weight(UserActivityEventType.SKIP)).isEqualTo(-1.0);
        assertThat(EventWeights.weight(UserActivityEventType.WISHLIST)).isEqualTo(3.0);
        assertThat(EventWeights.weight(UserActivityEventType.CART)).isEqualTo(4.0);
        assertThat(EventWeights.weight(UserActivityEventType.PURCHASE)).isEqualTo(5.0);
    }

    @Test
    void decayUsesTieredAgeBuckets() {
        assertThat(PreferenceDecay.factor(LocalDateTime.now().minusDays(3))).isEqualTo(1.0);
        assertThat(PreferenceDecay.factor(LocalDateTime.now().minusDays(14))).isEqualTo(0.8);
        assertThat(PreferenceDecay.factor(LocalDateTime.now().minusDays(45))).isEqualTo(0.6);
        assertThat(PreferenceDecay.factor(LocalDateTime.now().minusDays(120))).isEqualTo(0.3);
    }
}
