# How Sneaky Backend Communicates with Sneaky Recommender

`Sneaky_Backend` communicates with `Sneaky_Recommender` through an HTTP API.

## Communication Flow

1. `ProductRecommendationService` creates the initial product candidates using the existing point-based recommendation logic.
2. It converts each candidate into ML features such as:
   - Rule score
   - Price
   - Popularity rank
   - Brand, category, merchant, and price matches
   - Passed preference matches
   - Recently viewed, passed, and owned status
3. `ProductRecommendationService.mlRerank()` calls:

   ```java
   mlRankingClient.rank(user.getUserId(), features)
   ```

4. `HttpMlRankingClient` serializes the user ID and candidate features as JSON.
5. It sends an HTTP `POST` request to:

   ```text
   http://localhost:8090/rank
   ```

6. The FastAPI `/rank` endpoint in `Sneaky_Recommender` validates the request and passes the candidates to the loaded ML model.
7. The recommender returns every product ID with its predicted score and the model version.
8. The backend maps the returned scores to the products, scales the scores by `100`, and sorts the products from highest score to lowest score.

## Request Example

```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "candidates": [
    {
      "productId": "123e4567-e89b-12d3-a456-426614174001",
      "ruleScore": 75.0,
      "price": 4999.0,
      "popularityRank": 2,
      "brandMatches": 3,
      "categoryMatches": 2,
      "merchantMatches": 1,
      "priceMatches": 2,
      "passedBrandMatches": 0,
      "passedCategoryMatches": 0,
      "passedMerchantMatches": 0,
      "recentlyViewed": false,
      "passed": false,
      "owned": false
    }
  ]
}
```

## Response Example

```json
{
  "rankings": [
    {
      "productId": "123e4567-e89b-12d3-a456-426614174001",
      "score": 0.87
    }
  ],
  "modelVersion": "1.0.0"
}
```

## Backend Configuration

The integration is disabled by default. Enable it with these environment variables:

```bash
APP_ML_RECOMMENDATIONS_ENABLED=true
APP_ML_RECOMMENDATIONS_BASE_URL=http://localhost:8090
APP_ML_RECOMMENDATIONS_TIMEOUT=500ms
```

The corresponding Spring properties are:

```properties
app.recommendations.ml.enabled=${APP_ML_RECOMMENDATIONS_ENABLED:false}
app.recommendations.ml.base-url=${APP_ML_RECOMMENDATIONS_BASE_URL:http://localhost:8090}
app.recommendations.ml.timeout=${APP_ML_RECOMMENDATIONS_TIMEOUT:500ms}
```

## Failure Handling

The integration fails open. The backend keeps the original point-based candidate order when:

- ML recommendations are disabled.
- The recommender is unavailable.
- The request times out.
- The recommender returns a non-2xx response.
- The response is invalid or does not contain every candidate exactly once.
- No trained model is loaded and the recommender returns HTTP `503`.

This means the recommendation feature continues working even when the ML service is down.

## Relevant Files

- `ProductRecommendationService.java`: creates candidates and applies ML reranking.
- `MlRankingClient.java`: defines the backend/recommender data contract.
- `HttpMlRankingClient.java`: sends the HTTP request and validates the response.
- `Sneaky_Recommender/app/main.py`: provides the FastAPI `/rank` endpoint.
- `Sneaky_Recommender/app/model.py`: loads the model and predicts candidate scores.
