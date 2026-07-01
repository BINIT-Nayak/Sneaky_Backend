package com.sneaky.sneaky.services.recommendation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MlRankingClient {
    Optional<List<RankedCandidate>> rank(UUID userId, List<CandidateFeatures> candidates);

    record CandidateFeatures(
            UUID productId,
            double ruleScore,
            double price,
            int popularityRank,
            int brandMatches,
            int categoryMatches,
            int merchantMatches,
            int priceMatches,
            int passedBrandMatches,
            int passedCategoryMatches,
            int passedMerchantMatches,
            boolean recentlyViewed,
            boolean passed,
            boolean owned) {
    }

    record RankedCandidate(UUID productId, double score) {
    }
}
