package com.sneaky.sneaky.services.recommendation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
public class HttpMlRankingClient implements MlRankingClient {
    private final boolean enabled;
    private final URI rankingUri;
    private final Duration timeout;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public HttpMlRankingClient(
            @Value("${app.recommendations.ml.enabled:false}") boolean enabled,
            @Value("${app.recommendations.ml.base-url:http://localhost:8090}") String baseUrl,
            @Value("${app.recommendations.ml.timeout:500ms}") Duration timeout,
            ObjectMapper objectMapper) {
        this(enabled, baseUrl, timeout, objectMapper, HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build());
    }

    HttpMlRankingClient(
            boolean enabled,
            String baseUrl,
            Duration timeout,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.enabled = enabled;
        this.rankingUri = URI.create(stripTrailingSlash(baseUrl) + "/rank");
        this.timeout = timeout;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<List<RankedCandidate>> rank(UUID userId, List<CandidateFeatures> candidates) {
        if (!enabled || userId == null || candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        try {
            String body = objectMapper.writeValueAsString(new RankingRequest(userId, candidates));
            HttpRequest request = HttpRequest.newBuilder(rankingUri)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            RankingResponse rankingResponse = objectMapper.readValue(response.body(), RankingResponse.class);
            if (!isValidRanking(rankingResponse, candidates)) {
                return Optional.empty();
            }

            return Optional.of(rankingResponse.rankings());
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private static boolean isValidRanking(RankingResponse response, List<CandidateFeatures> candidates) {
        if (response == null || response.rankings() == null || response.rankings().size() != candidates.size()) {
            return false;
        }

        Set<UUID> expectedIds = new HashSet<>();
        for (CandidateFeatures candidate : candidates) {
            expectedIds.add(candidate.productId());
        }

        Set<UUID> rankedIds = new HashSet<>();
        for (RankedCandidate candidate : response.rankings()) {
            if (candidate == null
                    || candidate.productId() == null
                    || !Double.isFinite(candidate.score())
                    || !rankedIds.add(candidate.productId())) {
                return false;
            }
        }

        return rankedIds.equals(expectedIds);
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record RankingRequest(UUID userId, List<CandidateFeatures> candidates) {
    }

    private record RankingResponse(List<RankedCandidate> rankings, String modelVersion) {
    }
}
