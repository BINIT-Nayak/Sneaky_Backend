# Sneaky System Architecture

This document summarizes the backend architecture added for behavioral events, preference learning, Redis recommendation caching, and hybrid recommendations.

## Architecture Snapshot

```mermaid
flowchart TD
    FE[React Frontend]
    API[Spring Boot API]
    DB[(PostgreSQL)]
    Redis[(Redis / Upstash)]
    Kafka[Kafka<br/>sneaky.user-activity]
    Consumer[UserActivityEventConsumer]
    Profile[UserPreferenceProfileService]
    Engine[ProductRecommendationService]
    ML[Optional ML Recommender]

    FE -->|REST /api/*| API
    API --> DB
    API --> Redis
    API -->|publish activity events| Kafka
    Kafka --> Consumer
    Consumer --> Profile
    Profile --> DB
    Consumer -->|analytics + recent signals| Redis
    Consumer -->|invalidate user recommendation cache| Redis
    API --> Engine
    Engine --> DB
    Engine --> Redis
    Engine -. optional rerank .-> ML
```

## Recommendation Flow

```mermaid
flowchart TD
    Request[GET /api/products/recommended]
    Exclude{excludeIds present?}
    Cache[Read Redis recommendation cache]
    Hit{Cache hit?}
    Products[Load active approved products]
    User{Authenticated user?}
    Profile[Load preference profile]
    Signals[Load wishlist, cart, recent views, passes]
    Rules[Rule-based candidate scoring]
    ML{ML enabled and useful signals?}
    Rank[Optional ML reranking]
    Diversity[Diversity reranking]
    Save[Write Redis cache<br/>15 min TTL]
    Response[Return ProductDTO list]

    Request --> Exclude
    Exclude -- no --> Cache --> Hit
    Exclude -- yes --> Products
    Hit -- yes --> Response
    Hit -- no --> Products
    Products --> User
    User -- no --> Diversity
    User -- yes --> Profile --> Signals --> Rules --> ML
    ML -- yes --> Rank --> Diversity
    ML -- no --> Diversity
    Diversity --> Save --> Response
```

## Event Tracking Flow

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant API as UserEventController
    participant Producer as ActivityEventPublisher
    participant Kafka as Kafka Topic
    participant Consumer as UserActivityEventConsumer
    participant Profile as Preference Profile
    participant Redis as Redis Cache

    FE->>API: POST /api/events
    API->>Producer: publish UserActivityEventDTO
    API-->>FE: 202 Accepted
    Producer->>Kafka: sneaky.user-activity
    Kafka-->>Consumer: consume event
    Consumer->>Profile: update brand/category/price scores
    Consumer->>Redis: update analytics signals
    Consumer->>Redis: delete recommendations:user:{userId}
```

## User Preference Learning

```mermaid
flowchart LR
    Event[User event]
    Weight[Event weight]
    Product[Product brand/category/price]
    Brand[user_brand_preferences]
    Category[user_category_preferences]
    Price[user_preferences price range]
    Decay[Preference decay]
    Clamp[Clamp scores -1 to +1]

    Event --> Weight
    Event --> Product
    Weight --> Brand --> Decay --> Clamp
    Weight --> Category --> Decay --> Clamp
    Product --> Brand
    Product --> Category
    Product --> Price
```

Supported event types:

| Event | Weight |
| --- | ---: |
| `IMPRESSION` | 0.0 |
| `VIEW` | 0.5 |
| `CLICK` | 1.0 |
| `SKIP` | -1.0 |
| `WISHLIST` | 3.0 |
| `CART` | 4.0 |
| `PURCHASE` | 5.0 |

Preference decay keeps old behavior from dominating forever:

| Signal Age | Effective Weight |
| --- | ---: |
| Less than 7 days | 100% |
| 7-30 days | 80% |
| 30-90 days | 60% |
| 90+ days | 30% |

## Redis Usage

| Key / Area | Purpose | TTL |
| --- | --- | ---: |
| `recommendations:guest` | Guest feed ranked product IDs | 15 min |
| `recommendations:user:{userId}` | Personalized ranked product IDs | 15 min |
| `recommendations:user:{userId}:personalized` | Cache metadata flag | 15 min |
| Product analytics keys | Views, passes, popularity, recent signals | Feature-specific |
| Rate limit keys | API/session refresh throttling | Short window |
| Logout token keys | JWT invalidation support | Token lifetime |

## Why This Matters

| Architecture Change | Impact |
| --- | --- |
| Event API returns `202 Accepted` | User actions do not wait for recommendation processing |
| Kafka consumer updates preferences asynchronously | Behavior learning is decoupled from request latency |
| Redis cache-aside recommendation feed | Repeated feed opens avoid expensive ranking work |
| Event-driven cache invalidation | Wishlist/cart/purchase/preference updates refresh future recommendations |
| Hybrid recommendation engine | Rule-based fallback remains reliable while ML reranking can improve personalization |
| Preference decay | Old user behavior naturally loses influence over time |
