# Coffee Chat Microservices Platform - Architecture Context Document

## Project Overview
A production-ready backend microservices platform for storing and managing chat histories from a RAG (Retrieval-Augmented Generation) chatbot system. The platform includes user authentication with JWT, per-user API keys, and separate microservices for user management and chat functionality.

---

## Technology Stack

### Backend
- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.3.x
- **Database**: PostgreSQL 15 with pgvector extension for vector storage
- **Database Migration**: Flyway
- **LLM Integration**: Spring AI with Ollama-backed chat models
- **Security**: Spring Security with JWT (JJWT 0.12.5)
- **API Documentation**: SpringDoc OpenAPI (Swagger)
- **Rate Limiting**: Bucket4j
- **Logging**: Logback
- **Testing**: Spring Boot Test + TestContainers
- **Build Tool**: Gradle 8.x (multi-module: user-service, chat-service)
- **Code Formatter**: Spotless (google-java-format)
- **Docker**: Jib (daemonless Docker image builds)

### Frontend
- **Framework**: React 18
- **Language**: TypeScript 5
- **Build Tool**: Vite 5
- **Styling**: Tailwind CSS
- **HTTP Client**: Axios
- **Type Safety**: Full TypeScript support with strict mode
- **Code Formatter**: Prettier with ESLint integration

### Infrastructure
- **API Gateway**: Nginx (Alpine)
- **Containerization**: Docker + Docker Compose
- **Database Admin**: pgAdmin 4

---

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│              Nginx Gateway (Port 80)                   │
│                                                        │
│  Routes:                                               │
│  - /user/*  → user-service:8081                       │
│  - /chat/*  → chat-service:8082                       │
│  - /gateway/* → nginx itself                          │
└────────────────┬───────────────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
┌──────────────────┐           ┌──────────────────┐
│  User Service    │           │  Chat Service    │
│  Port: 8081      │           │  Port: 8082      │
│  Context: /user  │           │  Context: /chat  │
│                  │           │                  │
│  /user/api/auth  │           │  /chat/api/      │
│  /user/api/users │           │    sessions      │
│  /user/api/health│           │  /chat/api/health│
└────────┬─────────┘           └─────────┬────────┘
         │                               │
         └──────────────┬────────────────┘
                        ▼
┌─────────────────────────────────────────┐
│ Shared Postgres DB: ragchat_db         │
│ Schemas:                               │
│  - user_service (User Service)         │
│  - chat_service (Chat Service)         │
│ Exposed Port: 5433                     │
└─────────────────────────────────────────┘
```

---

## Microservices Breakdown

### 1. User Service (Port 8081, Context: /user)

**Responsibilities:**
- User signup/login with JWT authentication
- JWT token generation, validation, and refresh
- Per-user API key generation and management
- User profile CRUD operations
- Password hashing with BCrypt

**Database:** Shared Postgres DB `ragchat_db` (schema: `user_service`)
- `users` table

**Key Endpoints:**
```
POST   /user/api/auth/signup
POST   /user/api/auth/login
POST   /user/api/auth/refresh
GET    /user/api/auth/validate-token
GET    /user/api/users/me
PUT    /user/api/users/me
POST   /user/api/users/me/regenerate-api-key
GET    /user/api/users/me/api-key
GET    /user/api/health
GET    /user/swagger-ui.html
```

---

### 2. Chat Service (Port 8082, Context: /chat)

**Responsibilities:**
- Chat session management (CRUD operations)
- Message storage and retrieval with RAG context
- Orchestrate LLM-backed chat responses using Spring AI `ChatClient`
- Integrate with Ollama for local LLM inference
- Use pgvector in PostgreSQL as a vector store for RAG embeddings
- Session renaming and favorite marking
- User ownership validation
- Rate limiting per user

**Database:** Shared Postgres DB `ragchat_db` (schema: `chat_service`)
- `chat_sessions` table (with `user_id` FK reference)
- `chat_messages` table

**Key Endpoints:**
```
POST   /chat/api/sessions
GET    /chat/api/sessions
GET    /chat/api/sessions/{id}
PUT    /chat/api/sessions/{id}/rename
PUT    /chat/api/sessions/{id}/favorite
DELETE /chat/api/sessions/{id}
POST   /chat/api/sessions/{id}/messages
GET    /chat/api/sessions/{id}/messages
GET    /chat/api/health
GET    /chat/swagger-ui.html
```

---

### 3. Nginx Gateway (Port 80)

**Responsibilities:**
- Route requests to appropriate microservices
- Simple path-based routing (no URL rewriting)
- Delegate CORS handling to backend services (Spring Security)

**Routes:**
- `/user/*` → User Service
- `/chat/*` → Chat Service
- `/gateway/health` → Gateway health check
- `/gateway/routes` → List available routes

---

## Database Schema

### User Service Database

- **Schema**: `user_service`
- **Main table**: `users`
  - `id` (UUID, PK)
  - `username`, `email` (unique identifiers)
  - `password_hash`
  - `api_key` (per-user, unique)
  - `is_active`
  - `created_at`, `updated_at`
- Indexed on username, email, and api_key for fast lookup.

### Chat Service Database

- **Schema**: `chat_service`
- **Tables**:
  - `chat_sessions`
    - `id` (UUID, PK)
    - `user_id` (logical reference to `users.id`)
    - `session_name`
    - `is_favorite`
    - `created_at`, `updated_at`
  - `chat_messages`
    - `id` (UUID, PK)
    - `session_id` (FK to `chat_sessions.id`)
    - `sender` (`USER` or `AI`)
    - `content` (raw text from user or LLM)
    - `context` (JSONB for RAG metadata and embeddings linkage)
    - `message_order`
    - `created_at`, `updated_at`
- Tables are indexed by user, session, created_at, and message_order to support fast session listing and chronological message retrieval.

**Note:** Foreign key from `chat_sessions.user_id` to `users.id` is a **logical reference only** (not enforced via database FK). The tables live in different schemas and we intentionally avoid cross-schema constraints to keep microservices loosely coupled.

---

## Authentication Architecture

### Dual Authentication Support

**1. JWT Token Authentication** (for web/mobile apps)
- Access token expires after 24 hours
- Refresh token expires after 7 days
- Tokens contain: user_id, username, expiration

**2. API Key Authentication** (for programmatic access)
- Generated per user (64-character UUID-based)
- Permanent until regenerated
- Passed via `X-API-KEY` header

### Authentication Flow

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       │ 1. POST /user/api/auth/signup or /login
       ▼
┌──────────────────┐
│  User Service    │
│  - Hash password │
│  - Generate JWT  │
│  - Generate API  │
└──────┬───────────┘
       │
       │ 2. Returns: { token, refreshToken, apiKey, user }
       ▼
┌─────────────┐
│   Client    │ Stores tokens
└──────┬──────┘
       │
       │ 3. Subsequent requests with:
       │    Authorization: Bearer <JWT>
       │    OR X-API-KEY: <api-key>
       ▼
┌──────────────────┐
│  Chat Service    │
│  - Extract JWT   │
│  - Call User Svc │
│    validate-token│
└──────────────────┘
```

### Security Filters (Chat Service)

**Filter Chain Order:**
1. CorsFilter
2. ApiKeyAuthenticationFilter (checks X-API-KEY)
3. JwtAuthenticationFilter (checks Authorization: Bearer)
4. RateLimitInterceptor
5. Controller

---

## JPA Auditing

### Base Entity for Timestamps

- Both services use a shared `AuditableEntity` base class to provide `created_at` and `updated_at` columns.
- Spring Data JPA auditing is enabled so timestamps are managed automatically.
- No database triggers are required; auditing is applied uniformly across entities.

---

## Key Configuration Files

### User Service - application.yml (conceptual)

- Sets `server.port=8081` and `servlet.context-path=/user`.
- Configures the main Postgres datasource and Flyway migrations.
- Defines JWT properties (`jwt.secret`, expiration, refresh-expiration).

### Chat Service - application.yml (conceptual)

- Sets `server.port=8082` and `servlet.context-path=/chat`.
- Configures the shared Postgres datasource and Flyway migrations for `chat_service` schema.
- Points to User Service via `user-service.url` for token validation.
- Defines rate-limiting properties under `rate-limit.*`.
- Configures LLM integration:
  - `spring.ai.ollama.base-url` from `OLLAMA_HOST`.
  - `spring.ai.ollama.chat.options.model` from `OLLAMA_MODEL`.
- Configures vector store integration:
  - `spring.ai.vectorstore.pgvector.*` for pgvector schema, dimensions, and distance metric.

---

## Project Structure

```
rag-chat-platform/
│
├── nginx/
│   └── nginx.conf
│
├── user-service/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/ragchat/user/
│   │   │   │   ├── config/
│   │   │   │   │   ├── JpaAuditingConfig.java
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   └── SwaggerConfig.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── AuditableEntity.java
│   │   │   │   │   │   └── User.java
│   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── request/
│   │   │   │   │   │   │   ├── SignupRequest.java
│   │   │   │   │   │   │   └── LoginRequest.java
│   │   │   │   │   │   └── response/
│   │   │   │   │   │       ├── AuthResponse.java
│   │   │   │   │   │       └── UserResponse.java
│   │   │   │   │   └── enums/
│   │   │   │   ├── repository/
│   │   │   │   │   └── UserRepository.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   ├── JwtService.java
│   │   │   │   │   └── UserService.java
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   └── HealthController.java
│   │   │   │   ├── security/
│   │   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   ├── ApiKeyAuthenticationFilter.java
│   │   │   │   │   └── UserPrincipal.java
│   │   │   │   ├── exception/
│   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   └── [custom exceptions]
│   │   │   │   └── util/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/
│   │   │           ├── V1__create_users_table.sql
│   │   │           └── V2__add_indexes.sql
│   │   └── test/
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradle.properties
│   ├── gradlew
│   ├── gradlew.bat
│   └── README.md
│
├── chat-service/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/ragchat/chat/
│   │   │   │   ├── config/
│   │   │   │   │   ├── JpaAuditingConfig.java
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   ├── SwaggerConfig.java
│   │   │   │   │   └── RateLimitConfig.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── AuditableEntity.java
│   │   │   │   │   │   ├── ChatSession.java
│   │   │   │   │   │   └── ChatMessage.java
│   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── request/
│   │   │   │   │   │   └── response/
│   │   │   │   │   └── enums/
│   │   │   │   │       └── MessageSender.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── ChatSessionRepository.java
│   │   │   │   │   └── ChatMessageRepository.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── SessionService.java
│   │   │   │   │   └── MessageService.java
│   │   │   │   ├── controller/
│   │   │   │   │   ├── SessionController.java
│   │   │   │   │   ├── MessageController.java
│   │   │   │   │   └── HealthController.java
│   │   │   │   ├── security/
│   │   │   │   │   ├── JwtValidationFilter.java
│   │   │   │   │   └── RateLimitInterceptor.java
│   │   │   │   ├── client/
│   │   │   │   │   └── UserServiceClient.java
│   │   │   │   ├── exception/
│   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   └── [custom exceptions]
│   │   │   │   └── util/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/
│   │   │           ├── V1__create_chat_sessions_table.sql
│   │   │           ├── V2__create_chat_messages_table.sql
│   │   │           └── V3__add_indexes.sql
│   │   └── test/
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradle.properties
│   ├── gradlew
│   ├── gradlew.bat
│   └── README.md
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── auth/
│   │   │   │   ├── LoginForm.tsx
│   │   │   │   ├── SignupForm.tsx
│   │   │   │   └── ProtectedRoute.tsx
│   │   │   ├── profile/
│   │   │   │   ├── UserProfile.tsx
│   │   │   │   └── ApiKeyManager.tsx
│   │   │   ├── sessions/
│   │   │   │   ├── SessionList.tsx
│   │   │   │   └── SessionItem.tsx
│   │   │   └── chat/
│   │   │       ├── ChatWindow.tsx
│   │   │       ├── MessageBubble.tsx
│   │   │       └── MessageInput.tsx
│   │   ├── services/
│   │   │   └── api.ts
│   │   ├── types/
│   │   │   ├── auth.types.ts
│   │   │   ├── session.types.ts
│   │   │   ├── message.types.ts
│   │   │   └── api.types.ts
│   │   ├── hooks/
│   │   │   ├── useSessions.ts
│   │   │   └── useMessages.ts
│   │   ├── App.tsx
│   │   └── main.tsx
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   ├── tsconfig.json
│   ├── tsconfig.node.json
│   ├── .prettierrc.json
│   ├── .prettierignore
│   ├── .eslintrc.json
│   ├── .eslintignore
│   ├── package.json
│   └── README.md
│
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

---

## Environment Variables

### .env.example

```bash
# Shared Postgres Database
DB_NAME=ragchat_db
DB_USER=ragchat_user
DB_PASSWORD=secure_password

# Per-service schemas (no cross-schema constraints)
USER_DB_SCHEMA=user_service
CHAT_DB_SCHEMA=chat_service

# JWT Configuration (Shared)
JWT_SECRET=your-256-bit-secret-key-change-in-production-min-32-chars
JWT_EXPIRATION_MS=86400000        # 24 hours
JWT_REFRESH_EXPIRATION_MS=604800000  # 7 days

# Service URLs (with context paths)
USER_SERVICE_URL=http://user-service:8081/user
CHAT_SERVICE_URL=http://chat-service:8082/chat

# Rate Limiting
RATE_LIMIT_CAPACITY=100
RATE_LIMIT_REFILL_TOKENS=100
RATE_LIMIT_REFILL_DURATION=1

# PgAdmin
PGADMIN_EMAIL=admin@admin.com
PGADMIN_PASSWORD=admin123

# LLM
OLLAMA_MODEL=gemma3:1b
OLLAMA_HOST=http://host.docker.internal:11434
```

---

## Docker Compose Configuration

The backend stack is orchestrated via `backend/docker-compose.yml`, which defines:

- **nginx**: Gateway routing `/user/*` and `/chat/*` to the respective services.
- **db**: Shared Postgres instance with pgvector enabled.
- **user-service** and **chat-service**: Spring Boot microservices.
- **pgadmin**: Database administration UI.
- **llm**: Ollama container for LLM inference.

Refer to `backend/docker-compose.yml` for the exact service definitions, environment mappings, and port configuration.

---

## Inter-Service Communication

### Chat Service → User Service

- **Purpose:** Validate JWT tokens and load basic user details for authorization and personalization.
- **Mechanism:**
  - A `UserServiceClient` in chat-service calls `GET /user/api/auth/validate-token` on the User Service.
  - The call is made with `Authorization: Bearer <JWT>`.
  - On success, chat-service receives a `UserValidationResponse` (user id, username, email) and builds a `ChatUserPrincipal` stored in the Spring Security context.

This keeps authentication logic centralized in the User Service while allowing the Chat Service to remain stateless.

---

## API Response Standards

### Success Response
```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2025-11-15T10:30:00Z"
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "SESSION_NOT_FOUND",
    "message": "Session with id xyz not found",
    "timestamp": "2025-11-15T10:30:00Z",
    "path": "/chat/api/sessions/xyz"
  }
}
```

### Paginated Response
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": "2025-11-15T10:30:00Z"
}
```

---

## Testing Strategy

The backend uses a primarily **integration-test–driven** approach with real Postgres via Testcontainers:

- **Shared Testcontainers Config per Service**
  - `UserServicePostgresTestConfig` and `ChatServicePostgresTestConfig` bootstrap a Postgres 15 container.
  - Dynamic properties are registered for `SPRING_DATASOURCE_*`, service schemas, JWT secret, and rate-limit settings.

- **Service Integration Tests**
  - `UserServiceIT`, `SessionServiceIT`, `MessageServiceIT` exercise core service flows against a real database (create/update/favorite/delete sessions, create/list messages, regenerate API keys, etc.).

- **Controller Integration Tests (MockMvc)**
  - `UserControllerIT` and `AuthControllerIT` hit real HTTP endpoints with MockMvc, using API key and JWT authentication.
  - `SessionControllerIT` and `MessageControllerIT` hit chat-service endpoints, mocking `UserServiceClient` for token validation while still using real Postgres.
  - Negative-path tests are grouped into `@Nested` classes inside each controller IT (auth failures, validation errors, missing headers).
  - Rate limiting is verified in chat-service controller ITs using a test-specific rate-limit configuration and a focused "exceed bucket" test.

Unit tests can still be added where helpful, but most behavior is validated end-to-end at the HTTP + DB level within each microservice.

---

## Key Dependencies (backend/build.gradle)

At a high level, the multi-module backend build uses:

- **Spring Boot starters** for web, data JPA, security, and testing.
- **Database & migrations**: PostgreSQL driver, Flyway core + Postgres support.
- **JWT**: `io.jsonwebtoken:jjwt-*` for token creation and validation.
- **API documentation**: `springdoc-openapi-starter-webmvc-ui` for Swagger UI.
- **LLM & vector store**: Spring AI and pgvector dependencies (configured in chat-service module).
- **Build & packaging**: Gradle 8.x with Jib for container image builds.
- **Formatting & tooling**: Spotless (palantir-java-format), Lombok, and JUnit + Testcontainers for integration tests.

The Gradle multi-module setup and project properties are defined in `backend/settings.gradle` and `backend/gradle.properties`.

### Gradle Commands

**Build & Run:**
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Clean build
./gradlew clean build

# Build without tests
./gradlew build -x test

# Run tests only
./gradlew test
```

**Formatting:**
```bash
# Format all Java files in backend modules
cd backend
./gradlew spotlessApply

# Check formatting without modifying files
./gradlew spotlessCheck
```

**Docker Image Building with Jib:**
```bash
# Build and push to Docker daemon (local)
./gradlew jibDockerBuild

# Build and push to registry (requires authentication)
./gradlew jib

# Build to tar file
./gradlew jibBuildTar

# Build with custom image name
./gradlew jib --image=myregistry/myapp:1.0.0
```

**Other Commands:**
```bash
# List all tasks
./gradlew tasks

# Generate dependency report
./gradlew dependencies

# Show Jib configuration
./gradlew jibDockerBuild --dry-run
```

**IDE Integration:**

For IntelliJ IDEA:
1. Install "Palantir Java Format" plugin from marketplace
2. Go to Settings → Tools → Palantir Java Format
3. Enable "Format on save"
4. Set style to "Palantir"

For VS Code:
1. Install "Language Support for Java" extension
2. Add to settings.json:
```json
{
  "java.format.settings.url": "https://raw.githubusercontent.com/palantir/palantir-java-format/develop/idea-plugin/palantir-java-format.xml"
}
```

---

## Jib Docker Image Building

### Why Jib?

**Benefits over traditional Dockerfiles:**
- **No Docker daemon required** - Build images without Docker installed
- **Fast builds** - Only changed layers are rebuilt
- **Reproducible builds** - Deterministic image creation
- **Optimized layering** - Automatic separation of dependencies, resources, and classes
- **Multi-platform support** - Build for amd64 and arm64 simultaneously
- **Security** - Uses distroless or minimal base images
- **Integration** - Native Gradle/Maven plugin, no separate build step

### Jib Configuration Details

**Base Image:**
- Uses `eclipse-temurin:21-jre-alpine` for minimal footprint
- JRE-only (no JDK) reduces image size by ~200MB
- Alpine Linux for security and size (~50MB base)

**Multi-Architecture Support:**
- Builds for both `amd64` (Intel/AMD) and `arm64` (Apple Silicon, ARM servers)
- Single command creates multi-platform images

**JVM Optimization:**
- Initial heap: 512MB (`-Xms512m`)
- Maximum heap: 1GB (`-Xmx1024m`)
- G1 Garbage Collector for low-latency
- Fast startup with `/dev/urandom` entropy source

**Image Naming:**
- Format: `ragchat/[service-name]:[version]`
- Automatic tagging with version and `latest`
- Customizable via command line

### Building Images

**Local Development:**
```bash
# Build user-service image
cd user-service
./gradlew jibDockerBuild

# Build chat-service image
cd ../chat-service
./gradlew jibDockerBuild

# Verify images were created
docker images | grep ragchat
```

**Alternative: Build both services at once**
```bash
# From project root
cd user-service && ./gradlew jibDockerBuild && cd ..
cd chat-service && ./gradlew jibDockerBuild && cd ..
```

### Start Services
```bash
# Copy environment template
cp .env.example .env

# Edit environment variables
nano .env

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f user-service
docker-compose logs -f chat-service
docker-compose logs -f nginx
```

### Rebuild and Restart Services
```bash
# Rebuild images after code changes
cd user-service && ./gradlew jibDockerBuild && cd ..
cd chat-service && ./gradlew jibDockerBuild && cd ..

# Restart services with new images
docker-compose up -d --force-recreate user-service chat-service
```

### Stop Services
```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

---

## Testing Endpoints

### User Service
```bash
# Signup
curl -X POST http://localhost/user/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@test.com","password":"password123"}'

# Login
curl -X POST http://localhost/user/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"john","password":"password123"}'

# Health
curl http://localhost/user/api/health
```

### Chat Service
```bash
# Create session (requires JWT from login)
curl -X POST http://localhost/chat/api/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{"sessionName":"My Chat"}'

# Get sessions
curl http://localhost/chat/api/sessions \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Health
curl http://localhost/chat/api/health
```

### Gateway
```bash
# Gateway health
curl http://localhost/gateway/health

# List routes
curl http://localhost/gateway/routes

# API info
curl http://localhost/
```

---

## Swagger Documentation

- User Service: http://localhost/user/swagger-ui.html
- Chat Service: http://localhost/chat/swagger-ui.html

---

## Important Design Decisions

1. **Context Paths in Services**: Each service defines its own context path (`/user`, `/chat`) via `server.servlet.context-path`
2. **No URL Rewriting in Nginx**: Gateway does simple proxy_pass - services handle their own paths
3. **Separate Databases**: User and Chat services have isolated databases for data independence
4. **JPA Auditing**: Timestamps managed by JPA annotations, no database triggers needed
5. **Dual Authentication**: JWT for web apps, API keys for programmatic access
6. **Inter-Service REST**: Chat service calls User service via RestTemplate (not Feign)
7. **Rate Limiting**: Implemented with Bucket4j token bucket algorithm
8. **JSONB for Context**: RAG context stored as JSONB for flexibility
9. **UUID Primary Keys**: Better for distributed systems and security
10. **Cascade Deletes**: Handled at database level (ON DELETE CASCADE)
11. **Jib for Docker**: No Dockerfiles needed - Jib builds optimized images directly from Gradle
12. **Multi-Platform Images**: Jib automatically builds for both amd64 and arm64 architectures

---

## Core Requirements Fulfilled

✅ Store and manage chat sessions and messages  
✅ Session management (rename, favorite, delete)  
✅ User authentication with JWT + per-user API keys  
✅ API key authentication for all protected endpoints  
✅ Rate limiting with Bucket4j  
✅ Centralized logging with Logback  
✅ Global error handling with @RestControllerAdvice  
✅ Environment configuration via .env files  
✅ Dockerized with docker-compose  
✅ Health check endpoints  
✅ Swagger/OpenAPI documentation  
✅ Database management UI (pgAdmin)
✅ Unit tests with Spring Boot Test + TestContainers  
✅ CORS configuration at gateway level  
✅ Pagination support for sessions and messages  
✅ Production-ready architecture with microservices  
✅ Flyway database migrations  
✅ Best practices for scalability and security  
✅ Automated code formatting (Palantir Java Format for backend, Prettier for frontend)  
✅ ESLint integration with TypeScript for frontend code quality  
✅ CI/CD ready with formatting checks  

---

## Frontend Architecture

### Technology Stack
- **React 18** with TypeScript 5
- **Vite 5** for build tooling
- **Tailwind CSS** for styling
- **Axios** for HTTP requests with TypeScript types
- **React Router** for navigation (if needed)
- **Strict TypeScript** configuration for type safety

### TypeScript Benefits

The frontend uses TypeScript with strict mode enabled, providing:

1. **Type Safety**: Catch errors at compile time before they reach production
2. **IntelliSense**: Full autocomplete and documentation in your IDE
3. **Refactoring**: Safely rename and restructure code with confidence
4. **API Contract**: Types mirror backend DTOs, ensuring API compatibility
5. **Self-Documentation**: Types serve as inline documentation
6. **Reduced Bugs**: Eliminate common runtime errors like undefined properties
7. **Better Developer Experience**: Clear interfaces for all components and hooks

### TypeScript Type Definitions

The frontend defines a small set of TypeScript types that mirror the backend DTOs:

- **Auth types**: `SignupRequest`, `LoginRequest`, `AuthResponse`, `UserResponse`.
- **Session types**: `CreateSessionRequest`, `SessionResponse`, `RenameSessionRequest`.
- **Message types**: `MessageSender` enum, `CreateMessageRequest`, `MessageResponse`.
- **API wrapper types**: `ApiResponse<T>`, `ApiError`, `PaginatedResponse<T>`, `PaginationParams`.

These types live under `src/types/*` and provide a strongly-typed contract between the React UI and the backend APIs.

### API Service Structure

The frontend exposes a small, typed API client layer (`src/services/api.ts`) that:

- Uses Axios instances for **User Service** (`/user/api`) and **Chat Service** (`/chat/api`).
- Automatically attaches JWTs to outgoing requests via request interceptors.
- Handles token refresh on `401` responses by calling the backend refresh endpoint.
- Exposes `authAPI`, `sessionAPI`, and `messageAPI` helpers that return `ApiResponse<T>`-wrapped data.
 
### Frontend Environment & Tooling

- Frontend `.env*` files provide `VITE_API_URL` for local development and production deployments.
- Tooling such as Vite, ESLint, and Prettier are configured via standard frontend config files (`package.json`, `.prettierrc`, `.eslintrc`, etc.) to enforce consistent formatting and linting.
- These configurations support developer experience but are not essential to understand the system architecture.
```json
{
  "env": {
    "browser": true,
    "es2021": true
  },
  "extends": [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:react-hooks/recommended",
    "plugin:react/recommended",
    "plugin:react/jsx-runtime",
    "plugin:prettier/recommended"
  ],
  "parser": "@typescript-eslint/parser",
  "parserOptions": {
    "ecmaVersion": "latest",
    "sourceType": "module",
    "ecmaFeatures": {
      "jsx": true
    }
  },
  "plugins": [
    "@typescript-eslint",
    "react",
    "react-hooks",
    "react-refresh",
    "prettier"
  ],
  "rules": {
    "react-refresh/only-export-components": [
      "warn",
      { "allowConstantExport": true }
    ],
    "prettier/prettier": "error",
    "@typescript-eslint/no-explicit-any": "warn",
    "@typescript-eslint/no-unused-vars": [
      "error",
      { "argsIgnorePattern": "^_" }
    ],
    "react/prop-types": "off"
  },
  "settings": {
    "react": {
      "version": "detect"
    }
  }
}
```

**.eslintignore:**
```
node_modules/
dist/
build/
.vite/
*.config.js
*.config.ts
```

---

## Nginx Configuration Details

Nginx is configured as a lightweight API gateway:

- Proxies `/user/*` to `user-service:8081` and `/chat/*` to `chat-service:8082`.
- Exposes a simple gateway health endpoint and a routes listing under `/gateway/*`.
- Forwards standard headers (`Host`, `X-Real-IP`, `X-Forwarded-*`) to backend services.

The full configuration is defined in `backend/docker/nginx/nginx.conf` and is applied by the `nginx` service in `backend/docker-compose.yml`.

---

## Security Implementation Details

- Both services use Spring Security with stateless sessions and JWT/API-key authentication.
- User Service exposes `/api/auth/**` and user profile endpoints, with protected routes requiring authentication.
- Chat Service protects `/api/sessions/**` and message endpoints, relying on `JwtValidationFilter` to populate `ChatUserPrincipal`.
- CORS is configured to allow browser-based clients to call both services.

## Rate Limiting Implementation

- Chat Service applies per-user rate limiting using **Bucket4j**.
- Limits are configured via `rate-limit.capacity`, `rate-limit.refill-tokens`, and `rate-limit.refill-duration-minutes`.
- A Spring MVC interceptor checks each request, consuming from a per-user bucket and returning `429 Too Many Requests` when exhausted.

## Flyway Migration Strategy

- Each service owns its schema (`user_service`, `chat_service`) and manages it via versioned Flyway migrations.
- Migrations create core tables (`users`, `chat_sessions`, `chat_messages`), indexes, and comments.
- Chat Service migrations also configure JSONB and pgvector usage for RAG context and embeddings.

---

## Entity Relationships

- **User**: represents an authenticated user with credentials, API key, and audit fields.
- **ChatSession**: belongs to a user and groups messages under a human-readable name with favorite status.
- **ChatMessage**: belongs to a session and is either `USER` or `AI` sender; stores content plus optional RAG `context` as JSON.
- **MessageSender**: enum capturing `USER` vs `AI` roles.

These entities mirror the database schema described earlier and are shared conceptually between backend and frontend DTOs.

## Global Exception Handling

- Each service exposes a `GlobalExceptionHandler` that:
  - Normalizes error responses into a standard `ApiResponse` format.
  - Maps domain-specific exceptions (e.g., session not found, unauthorized, rate limit exceeded) to appropriate HTTP status codes.
  - Collects validation errors into a field → message map for client consumption.

---

## Repository and DTO Overview

- **Repositories**:
  - `UserRepository` provides lookup by username, email, and API key.
  - `ChatSessionRepository` supports listing sessions by user, favorites, and recency.
  - `ChatMessageRepository` supports ordered retrieval and counting of messages per session.

- **DTOs**:
  - Request DTOs encapsulate validated input for signup, login, session creation/rename, and message creation.
  - Response DTOs expose stable JSON shapes for users, sessions, messages, and API responses (`ApiResponse`, `ErrorDetails`).

---

## Key Service Methods

JWT-related logic is encapsulated in a `JwtService` that:

- Generates signed JWTs with user identifiers and expiration based on `jwt.*` configuration.
- Validates and parses tokens using JJWT, throwing errors for invalid or expired tokens.
- Extracts claims like username and userId for downstream security components.

---

## Important Notes

- Context paths (`/user`, `/chat`) are defined in services, NOT rewritten in Nginx
- JWT secret must be the same across both services for validation
- User-Chat FK is logical only (not database-enforced) due to separate databases
- All timestamps managed by JPA Auditing, no database triggers
- Rate limiting is per-user/API-key, stored in-memory (consider Redis for production)
- CORS handled at Nginx level for simplicity
- Swagger accessible at `/user/swagger-ui.html` and `/chat/swagger-ui.html`
- Health checks at `/user/api/health`, `/chat/api/health`, `/gateway/health`
- Frontend uses TypeScript with strict mode for full type safety
- All API calls are fully typed with request/response interfaces
- Path aliases configured in tsconfig.json for cleaner imports
- Backend uses Gradle 8.x as build tool (not Maven)
- Backend uses Jib for Docker image building (no Dockerfiles required)
- Jib builds optimized, layered images with multi-platform support (amd64/arm64)
- Docker images must be built with `./gradlew jibDockerBuild` before running docker-compose

This completes the architecture overview for the Coffee Chat platform. Use this document as a reference for system design, service boundaries, and key integration points.