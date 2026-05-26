# Sneaky Backend 🚀

A modern, scalable, and secure backend application built using **Spring Boot**, designed to power the Sneaky platform with high performance, robust authentication, and clean architecture.

---

# 📌 Overview

**Sneaky Backend** is a RESTful backend service developed with **Java 21** and **Spring Boot**.  
The project focuses on scalability, maintainability, and secure API development while following clean backend engineering practices.

The backend handles:

- User authentication & authorization
- Secure JWT-based access management
- Database operations with JPA & PostgreSQL
- Redis caching support
- Product recommendations for the home feed
- Product analytics and recently viewed products
- Cart and wishlist management
- Validation & security layers
- Modular API architecture

---

# ✨ Features

- 🔐 JWT Authentication & Authorization
- ⚡ High-performance REST APIs
- 🛡️ Spring Security integration
- 🗄️ PostgreSQL database support
- 🚀 Redis caching integration
- 🧠 Rule-based product recommendation model
- 🧪 Product catalog seeding for recommendation testing
- ❤️ Wishlist APIs, including clear-all and one-call move-to-cart support
- 🛒 Cart APIs
- 📈 Product analytics counters and recently viewed products
- 📦 Clean layered architecture
- ✅ Request validation
- 🔄 Scalable service structure
- 🧩 JPA & Hibernate ORM support
- 📜 Production-ready configuration

---

# 🛠️ Tech Stack

## Backend Framework
- Java 21
- Spring Boot 4

## Spring Modules
- Spring Boot Web MVC
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Data Redis

## Database
- PostgreSQL

## Authentication
- JWT (JSON Web Token)

## Build Tool
- Maven

## Additional Libraries
- Lombok
- Hibernate
- JJWT

---

# 📂 Project Structure

```bash
Sneaky_Backend/
│
├── src/
│   ├── main/
│   │   ├── java/com/sneaky/
│   │   │   ├── config/          # Application configurations
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── repository/      # Database repositories
│   │   │   ├── security/        # JWT & Spring Security configs
│   │   │   ├── service/         # Business logic layer
│   │   │   ├── exception/       # Custom exception handling
│   │   │   └── SneakyApplication.java
│   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application.properties
│
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md

```

## ⚙️ Environment Configuration

Configure your database and JWT settings inside:

```bash
src/main/resources/application.properties
```

## 🚀 Getting Started
Prerequisites

Make sure you have installed:

- Java 21
- Maven
- PostgreSQL
- Redis
- Git

## 🔐 Authentication

Sneaky Backend uses JWT Authentication for secure API access.

- Authentication Flow
- User logs in
- Server generates JWT token
- Client stores token
- Protected APIs require Bearer Token

## 🗄️ Database

The application uses:

- PostgreSQL as the primary database
- Spring Data JPA for ORM
- Hibernate for entity management

## 🚀 Redis Integration

Redis is integrated for:

- Caching
- Product analytics counters
- Most-viewed product ranking
- Recently viewed products and product analytics counters

## 🧠 Product Recommendations

The home feed can request recommended products through:

```http
GET /api/products/recommended
```

The recommendation service ranks active products using:

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

If the user is logged out or has no history, the endpoint falls back to popularity and newest products.

The regular product endpoint is still available:

```http
GET /api/products
```

## 🧪 Product Catalog Seeding

`ProductCatalogSeeder` creates a larger product pool for local development and recommendation testing.

By default, it seeds up to `300` active products across multiple brands, categories, colors, sizes, and price bands.

Configure the minimum active product count with:

```bash
APP_SEED_PRODUCTS_MINIMUM_COUNT=300
```

Disable product seeding with:

```bash
APP_SEED_PRODUCTS_ENABLED=false
```

Current seed data includes:

- 20 brands
- 14 product categories
- Dummy merchant partners with links like `https://partners.sneaky.test/amazon`
- Budget, mid-range, and premium price bands
- Multiple size and color sets

Products also support merchant metadata:

- `merchantName`
- `merchantUrl`

If a product has no merchant URL, the backend falls back to `https://www.google.com/`.

## 📈 Kafka Product Analytics

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

## 🛒 Merchant Checkout

Sneaky does not process payments. Cart items include product merchant fields, and the frontend groups cart items by merchant so one outbound button appears per partner:

- Amazon items share one Amazon button
- Myntra items share one Myntra button
- AJIO, Nike, Puma, Adidas, and other partners follow the same pattern

The merchant buttons open the product partner site in a new tab.

## ❤️ Wishlist API

Wishlist endpoints require authentication:

```http
GET /api/wishlist
POST /api/wishlist
POST /api/wishlist/{productId}/move-to-cart
DELETE /api/wishlist/{productId}
DELETE /api/wishlist
```

`DELETE /api/wishlist` clears every wishlist item for the current user.

`POST /api/wishlist/{productId}/move-to-cart` adds or increments the product in the cart and removes it from the wishlist in one transaction. The endpoint returns the updated cart item.

## 🛡️ Security Features

- JWT Token Authentication
- Password encryption
- Spring Security filters
- Role-based authorization
- Secure API endpoints
- Request validation

## 📜 Maven Dependencies
Major Dependencies Used
- Dependency
- spring-boot-starter-webmvc	:REST API development
- spring-boot-starter-security	:Security & authentication
- spring-boot-starter-data-jpa	:Database ORM
- spring-boot-starter-validation	:Request validation
- spring-boot-starter-data-redis	:Redis integration
- postgresql	:PostgreSQL driver
- jjwt	:JWT token handling
- lombok	:Boilerplate reduction

## 📋 Future Improvements
-  API rate limiting
-  Docker Compose support
-  CI/CD pipeline integration
-  Monitoring & logging improvements

## ⭐ Support

If you found this project useful, give it a ⭐ on GitHub!
