# Product Recommendations

The home feed can request recommended products through:

```http
GET /api/products/recommended
```

The recommendation API is cache-first. It first reads precomputed ranked product IDs from Redis:

- `recommendations:guest`
- `recommendations:user:{userId}`

If cached rankings exist, the API only fetches those product records and returns them in ranked order. If the cache is empty or Redis is unavailable, it falls back to live scoring and writes the ranked IDs back to Redis with a 15-minute TTL.

The candidate model ranks active products using:

- Wishlist history
- Cart history
- Recently viewed products
- Products and categories the user passed
- Brand similarity
- Category similarity
- Merchant affinity and merchant pass penalties
- Similar price range
- Global popularity from Redis most-viewed analytics
- Diversity reranking to avoid repeating the same category, brand, or merchant in a tight loop

For personalized users, Sneaky can optionally send those candidates and their engineered interaction features to the standalone `Sneaky_Recommender` service. A trained model predicts interaction probability, after which this backend still applies diversity reranking and Redis caching. Guests and users without enough history stay on popularity ranking.

Enable ML reranking only after the service reports a loaded model from `GET /health`:

```bash
APP_ML_RECOMMENDATIONS_ENABLED=true
APP_ML_RECOMMENDATIONS_BASE_URL=http://localhost:8090
APP_ML_RECOMMENDATIONS_TIMEOUT=500ms
```

The integration is fail-open: disabled ML, timeouts, service errors, incomplete responses, and invalid scores all retain the existing rule-based candidate order.

If the user is logged out or has no history, the endpoint falls back to popularity and newest products.

When Kafka analytics is enabled, the consumer refreshes a user's recommendation cache after activity events are recorded. This moves expensive ranking work out of the request path and keeps the home feed fast under a larger product catalog.

The regular product endpoint is still available:

```http
GET /api/products
```
