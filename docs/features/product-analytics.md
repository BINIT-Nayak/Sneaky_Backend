# Product Analytics

Kafka support is opt-in for local development. When Kafka is enabled, the backend publishes user activity events for product views, cart actions, and wishlist actions to `sneaky.user-activity`. A Kafka consumer processes those events into Redis.

Kafka publishing runs outside the API request thread, so local Kafka latency should not block cart, wishlist, or product responses.

Enable it with:

```bash
APP_KAFKA_ENABLED=true
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
APP_KAFKA_TOPIC_USER_ACTIVITY=sneaky.user-activity
```

Available analytics endpoints:

- `GET /api/product-analytics/products/{productId}`
- `GET /api/product-analytics/recently-viewed`

Product views are tracked when an authenticated request calls `GET /api/products/{id}`.

Home feed passes are tracked with:

```http
POST /api/product-analytics/products/{productId}/pass
```

Passed products are stored per user in Redis and used by the recommendation model to reduce similar categories, brands, and merchants.
