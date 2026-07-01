package com.sneaky.sneaky.services.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class HttpMlRankingClientTest {

    @Test
    void rankReturnsValidatedModelScores() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        String responseBody = """
                {"rankings":[
                  {"productId":"%s","score":0.9},
                  {"productId":"%s","score":0.2}
                ],"modelVersion":"test-v1"}
                """.formatted(secondId, firstId);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(responseBody);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        HttpMlRankingClient client = new HttpMlRankingClient(
                true,
                "http://localhost:8090",
                Duration.ofSeconds(1),
                new ObjectMapper(),
                httpClient);
        List<MlRankingClient.CandidateFeatures> candidates = List.of(
                candidate(firstId, 20.0),
                candidate(secondId, 5.0));

        var result = client.rank(userId, candidates);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow())
                .extracting(MlRankingClient.RankedCandidate::productId)
                .containsExactly(secondId, firstId);
    }

    private static MlRankingClient.CandidateFeatures candidate(UUID productId, double ruleScore) {
        return new MlRankingClient.CandidateFeatures(
                productId,
                ruleScore,
                10000,
                2,
                1,
                1,
                0,
                1,
                0,
                0,
                0,
                false,
                false,
                false);
    }
}
