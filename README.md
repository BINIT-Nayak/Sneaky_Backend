# Sneaky Backend 🚀

A modern, scalable, and secure backend application built using **Spring Boot**, designed to power the Sneaky platform with high performance, robust authentication, and clean architecture.

LIVE: https://sneaky-4zjb.onrender.com/
---

# 📌 Overview

**Sneaky Backend** is a RESTful backend service developed with **Java 21** and **Spring Boot**.  
The project focuses on scalability, maintainability, and secure API development while following clean backend engineering practices.

The backend handles:

- User authentication & authorization
- Secure JWT-based access management
- Database operations with JPA & PostgreSQL
- Redis caching support
- Hybrid product recommendations for the home feed
- Kafka-based behavioral event tracking
- Aggregated user preference profiles
- Redis recommendation cache with event-driven invalidation
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
- 🧠 Hybrid recommendation engine with rule-based fallback and optional ML reranking
- 📡 Kafka event tracking with asynchronous preference updates
- 🎯 User preference profiles for brand, category, price, and behavior signals
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
- Spring Kafka

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
├── docs/
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
- Precomputed recommendation rankings
- Recommendation cache metadata and invalidation
- Rate limiting and logout token checks

## 🧠 Product Recommendations

The home feed can request ranked products through:

```http
GET /api/products/recommended
```

The recommendation system is hybrid:

- Redis cache-aside reads first for fast repeat feed loads
- rule-based candidate scoring uses popularity, preference profile, cart, wishlist, recent views, passed products, brand/category/merchant affinity, price fit, and diversity
- optional ML reranking can rerank candidates when enabled
- fallback stays available when Redis or ML is unavailable

Recommendation details live in [docs/features/recommendations.md](docs/features/recommendations.md).

## 🧭 System Architecture

The current architecture includes event tracking, Kafka, Redis recommendation cache, user preference profile aggregation, and optional ML reranking.

Architecture docs:

- [System architecture](docs/features/system-architecture.md)

## 📈 Kafka Product Analytics

Kafka analytics is opt-in and feeds Redis counters, user preference profiles, and recommendation cache invalidation. Event APIs return `202 Accepted` so product interactions are not blocked by downstream recommendation work. See [docs/features/product-analytics.md](docs/features/product-analytics.md).

## 🛒 Merchant Checkout

Sneaky does not process payments. Cart items include merchant metadata that the frontend uses for outbound partner checkout links. See [docs/features/commerce.md](docs/features/commerce.md).

## 📦 Product Import

Runtime product seeding has been removed. To populate a small database, use the admin API importer:

```bash
SNEAKY_API_BASE_URL=https://YOUR_BACKEND_DOMAIN \
SNEAKY_ADMIN_EMAIL=admin@example.com \
SNEAKY_ADMIN_PASSWORD=your_password \
SNEAKY_TARGET_PRODUCT_COUNT=80 \
node scripts/import-products-via-admin.mjs
```

The script logs in as an admin, creates missing brands, and adds products through `/api/admin/products`.

## ❤️ Wishlist API

Wishlist supports clear-all and one-call move-to-cart behavior. See [docs/features/commerce.md](docs/features/commerce.md).

## 🔔 Cart Reminders and Notifications

Cart reminders create in-app notifications and optional email reminders for cart items older than the configured age. See [docs/features/cart-reminders-and-notifications.md](docs/features/cart-reminders-and-notifications.md).

## 📚 Feature Docs

- [Recommendations](docs/features/recommendations.md)
- [System architecture](docs/features/system-architecture.md)
- [Product analytics](docs/features/product-analytics.md)
- [Commerce APIs](docs/features/commerce.md)
- [Cart reminders and notifications](docs/features/cart-reminders-and-notifications.md)

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
- spring-boot-starter-mail	:Cart reminder emails
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
