package com.sneaky.sneaky.services.analytics;

import java.math.BigDecimal;
import java.util.Map;

public record UserPreferenceProfile(
        Map<String, Double> brandScores,
        Map<String, Double> categoryScores,
        BigDecimal preferredPriceMin,
        BigDecimal preferredPriceMax,
        long totalInteractions) {

    public static UserPreferenceProfile empty() {
        return new UserPreferenceProfile(Map.of(), Map.of(), null, null, 0);
    }
}
