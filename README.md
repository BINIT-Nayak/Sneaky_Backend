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
- Validation & security layers
- Modular API architecture

---

# ✨ Features

- 🔐 JWT Authentication & Authorization
- ⚡ High-performance REST APIs
- 🛡️ Spring Security integration
- 🗄️ PostgreSQL database support
- 🚀 Redis caching integration
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
- Session storage
- Performance optimization
- Recently viewed products and product analytics counters

## 📈 Kafka Product Analytics

Kafka support is opt-in for local development. When Kafka is enabled, the backend publishes user activity events for product views, cart actions, and wishlist actions to `sneaky.user-activity`. A Kafka consumer processes those events into Redis.

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
- 🛡️ Security Features
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
