# Coffee Chat Microservices Platform

Production-ready backend microservices platform for storing and managing chat histories from a RAG (Retrieval-Augmented Generation) chatbot system.

## 🏗️ Architecture

- **User Service** (Port 8081): JWT authentication, user management, API key generation
- **Chat Service** (Port 8082): Chat session and message management, LLM chat orchestration using Spring AI + Ollama, and RAG context stored in PostgreSQL/pgvector
- **Nginx Gateway** (Port 80): API gateway with routing
- **PostgreSQL**: Single instance with separate schemas for User and Chat services
- **React Frontend**: TypeScript-based UI with full type safety

## 🚀 Technology Stack

### Backend
- Java 21 + Spring Boot 3.3.x
- PostgreSQL with Flyway migrations and pgvector extension for vector storage
- Spring AI + Ollama for LLM chat responses
- JWT authentication (JJWT 0.12.5)
- Gradle 8.x with Jib for Docker builds
- Spotless (google-java-format) for code style

### Frontend
- React 18 + TypeScript 5
- Vite 5 + Tailwind CSS
- Axios with full type safety
- Prettier + ESLint

### Infrastructure
- Docker + Docker Compose
- Nginx (Alpine)
- pgAdmin 4

## 📦 Project Structure

```
coffee-chat/
├── backend/                 # Backend multi-module Gradle project + Docker stack
│   ├── user-service/        # User authentication & management ✅
│   ├── chat-service/        # Chat sessions & messages ✅
│   ├── docker-compose.yml   # Multi-container orchestration (backend services + db + nginx)
│   ├── docker/
│   │   ├── db/              # Postgres init scripts + host volume
│   │   └── nginx/           # Nginx gateway configuration
│   ├── build.gradle         # Parent configuration
│   ├── settings.gradle      # Module definitions
│   └── gradlew             # Gradle wrapper
├── frontend/                # React TypeScript UI (core chat flow implemented)
├── IMPLEMENTATION_STATUS.md
├── NEXT_STEPS.md
└── README.md
```

## 🛠️ Implementation Status

### ✅ Completed
- **Root Configuration**: Docker Compose, Nginx, environment files
- **Multi-Module Gradle Setup**: Backend with shared configuration
- **User Service**:
  - Complete Spring Boot application
  - JWT + API Key authentication
  - User CRUD operations
  - Flyway database migrations
  - Swagger documentation
  - Security filters and configuration
  - Exception handling
- **Chat Service**:
  - Chat session and message management
  - LLM chat integration using Spring AI `ChatClient` and Ollama
  - pgvector-based vector store configured in shared PostgreSQL for RAG context
  - User token validation via User Service
  - Rate limiting with Bucket4j
- **Backend Testing**:
  - Testcontainers-backed integration tests for User and Chat services
  - Controller-level MockMvc tests with negative-path and rate-limit coverage

### 🚧 To Be Completed
1. **Frontend**: Profile & API key management UI, session rename/favorite/delete, and tests
2. **End-to-End Testing**: E2E flows across gateway, backend, and frontend

## 🏃 Quick Start

### Prerequisites
- Java 21
- Docker & Docker Compose
- Node.js 18+ (for frontend)

### Setup

1. **Clone and configure backend environment**:
```bash
cd coffee-chat
cd backend
cp .env.example .env
# Edit .env with your configuration (DB, JWT, LLM/Ollama, rate limiting, pgAdmin)
```

2. **Build backend Docker images**:
```bash
# Build all services
./gradlew jibDockerBuild

# Or build specific service
./gradlew :user-service:jibDockerBuild
```

4. **Start services**:
```bash
# From backend directory
cd backend
docker compose up -d
```

### Access Points
- **Gateway**: http://localhost/
- **User Service Swagger**: http://localhost/user/swagger-ui.html
- **Chat Service Swagger**: http://localhost/chat/swagger-ui.html
- **PgAdmin**: http://localhost:5050

## 🧠 LLM Chat Architecture

- The **Chat Service** uses Spring AI's `ChatClient` to call an Ollama server configured via `OLLAMA_HOST`.
- The LLM model is configured via `OLLAMA_MODEL` (for example, `gemma3:1b`) in `backend/.env`.
- Chat messages (USER and AI) are stored in `chat_messages`, with optional RAG `context` metadata.
- Spring AI's pgvector integration is configured against the shared PostgreSQL database (schema `chat_service`) to support vector storage for retrieval-augmented chat flows.

## 📚 API Documentation

### User Service Endpoints

**Authentication:**
- `POST /user/api/auth/signup` - Register new user
- `POST /user/api/auth/login` - Login
- `POST /user/api/auth/refresh` - Refresh token
- `GET /user/api/auth/validate-token` - Validate JWT

**User Management:**
- `GET /user/api/users/me` - Get current user
- `PUT /user/api/users/me` - Update profile
- `POST /user/api/users/me/regenerate-api-key` - Regenerate API key
- `GET /user/api/users/me/api-key` - Get API key

**Health:**
- `GET /user/api/health` - Service health check

### Chat Service Endpoints

**Sessions:**
- `POST /chat/api/sessions` - Create session
- `GET /chat/api/sessions` - List sessions
- `GET /chat/api/sessions/{id}` - Get session
- `PUT /chat/api/sessions/{id}/rename` - Rename session
- `PUT /chat/api/sessions/{id}/favorite` - Toggle favorite
- `DELETE /chat/api/sessions/{id}` - Delete session

**Messages:**
- `POST /chat/api/sessions/{id}/messages` - Add message
- `GET /chat/api/sessions/{id}/messages` - Get messages

## 🔐 Authentication

Two authentication methods supported:

1. **JWT Token** (for web/mobile apps):
   - Header: `Authorization: Bearer <token>`
   - Expires: 24 hours
   - Refresh token: 7 days

2. **API Key** (for programmatic access):
   - Header: `X-API-KEY: <api-key>`
   - Permanent until regenerated

## 🧪 Testing

```bash
# From backend directory
cd backend

# Run all backend tests (user-service + chat-service)
./gradlew test

# Run tests for a specific service
./gradlew :user-service:test
./gradlew :chat-service:test
```

## 🔧 Development

### Build without Docker
```bash
cd backend

# Run user service (reads env from backend/.env)
./gradlew :user-service:bootRun

# Run chat service (reads env from backend/.env)
./gradlew :chat-service:bootRun
```

### Format code
```bash
cd backend

# Format all backend modules
./gradlew spotlessApply

# Check formatting (for CI)
./gradlew spotlessCheck
```

##  Environment Variables

See `backend/.env.example` for all backend configuration options. Copy it to `backend/.env` for local development and Docker Compose:

- `DB_NAME`, `DB_USER`, `DB_PASSWORD`: Shared Postgres database credentials
- `USER_DB_SCHEMA`, `CHAT_DB_SCHEMA`: Per-service schemas in the shared DB
- `JWT_SECRET`: Base64-encoded secret key for JWT signing
- `RATE_LIMIT_*`: Rate limiting configuration for Chat Service
- `OLLAMA_MODEL`, `OLLAMA_HOST`: LLM model and host used by Chat Service via Spring AI + Ollama

## 🤝 Contributing

1. Use Spotless (google-java-format) for backend code (`cd backend && ./gradlew spotlessApply`)
2. Use Prettier for frontend code
3. Write tests for new features
4. Update documentation

## 📄 License

MIT License - see LICENSE file for details

## 🔗 Resources

- [Architecture Document](docs/architecture.md) - Complete system design
- [Implementation Status](IMPLEMENTATION_STATUS.md) - Detailed progress tracking
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev/)

---

**Status**: User Service and Chat Service complete, core frontend chat flow implemented.
