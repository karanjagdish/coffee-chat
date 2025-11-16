# User Service

JWT-based authentication and user management microservice for the Coffee Chat Platform.

## Features

- ✅ User registration and login
- ✅ JWT token generation and validation
- ✅ API key generation for programmatic access
- ✅ Dual authentication (JWT + API Key)
- ✅ User profile management
- ✅ BCrypt password hashing
- ✅ Flyway database migrations
- ✅ Swagger/OpenAPI documentation
- ✅ Global exception handling
- ✅ Spring Security integration

## Technology Stack

- **Java 21** with Spring Boot 3.3.5
- **PostgreSQL 15** with Flyway migrations
- **JWT** (JJWT 0.12.5) for authentication
- **Gradle 8.x** with Jib for Docker builds
- **Palantir Java Format** for code style

## Project Structure

```
user-service/
├── src/main/java/com/ragchat/user/
│   ├── config/              # Spring configuration
│   │   ├── JpaAuditingConfig.java
│   │   ├── SecurityConfig.java
│   │   └── SwaggerConfig.java
│   ├── controller/          # REST controllers
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   └── HealthController.java
│   ├── model/
│   │   ├── entity/          # JPA entities
│   │   │   ├── AuditableEntity.java
│   │   │   └── User.java
│   │   └── dto/             # Data Transfer Objects
│   │       ├── request/
│   │       └── response/
│   ├── repository/          # JPA repositories
│   │   └── UserRepository.java
│   ├── security/            # Security filters
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── ApiKeyAuthenticationFilter.java
│   │   └── UserPrincipal.java
│   ├── service/             # Business logic
│   │   ├── AuthService.java
│   │   ├── JwtService.java
│   │   └── UserService.java
│   ├── exception/           # Exception handling
│   │   ├── GlobalExceptionHandler.java
│   │   └── [custom exceptions]
│   ├── util/                # Utilities
│   │   └── ApiKeyGenerator.java
│   └── UserServiceApplication.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/        # Flyway migrations
│       ├── V1__create_users_table.sql
│       └── V2__add_indexes.sql
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## API Endpoints

### Authentication

**POST /user/api/auth/signup**
- Register a new user
- Returns JWT token, refresh token, and API key

**POST /user/api/auth/login**
- Login with username/email and password
- Returns JWT token, refresh token, and API key

**POST /user/api/auth/refresh**
- Refresh access token using refresh token

**GET /user/api/auth/validate-token**
- Validate JWT token (used by Chat Service)
- Requires: `Authorization: Bearer <token>`

### User Management

**GET /user/api/users/me**
- Get current user profile
- Requires authentication

**PUT /user/api/users/me**
- Update user profile (username, email)
- Requires authentication

**POST /user/api/users/me/regenerate-api-key**
- Generate new API key
- Requires authentication

**GET /user/api/users/me/api-key**
- Get current API key
- Requires authentication

### Health Check

**GET /user/api/health**
- Service health check
- No authentication required

## Authentication Methods

### 1. JWT Token (Web/Mobile Apps)

```bash
curl http://localhost/user/api/users/me \
  -H "Authorization: Bearer <jwt-token>"
```

- **Expires**: 24 hours
- **Refresh Token**: 7 days

### 2. API Key (Programmatic Access)

```bash
curl http://localhost/user/api/users/me \
  -H "X-API-KEY: <api-key>"
```

- **Expires**: Never (until regenerated)

## Database Schema

### users table

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PRIMARY KEY |
| username | VARCHAR(50) | NOT NULL, UNIQUE |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| password_hash | VARCHAR(255) | NOT NULL |
| api_key | VARCHAR(64) | NOT NULL, UNIQUE |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE |
| created_at | TIMESTAMP | NOT NULL (JPA managed) |
| updated_at | TIMESTAMP | (JPA managed) |

**Indexes:**
- `idx_users_username` on username
- `idx_users_email` on email
- `idx_users_api_key` on api_key
- `idx_users_is_active` on is_active (partial)

## Building & Running

### Prerequisites

- Java 21
- Docker (for containerized deployment)
- PostgreSQL 15 (for local development)

### Local Development

```bash
# Build
./gradlew clean build

# Run
./gradlew bootRun

# Format code
./gradlew javaFormat

# Check formatting
./gradlew javaFormatCheck
```

### Docker Build

```bash
# Build Docker image with Jib
./gradlew jibDockerBuild

# Verify image
docker images | grep ragchat/user-service
```

### Run with Docker Compose

```bash
# From project root
docker-compose up -d user-service

# View logs
docker-compose logs -f user-service
```

## Configuration

### Environment Variables

Required environment variables (set in `.env` or docker-compose):

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://user-db:5432/user_service_db
SPRING_DATASOURCE_USERNAME=user_service_user
SPRING_DATASOURCE_PASSWORD=secure_password_1
JWT_SECRET=your-256-bit-secret-key-min-32-chars
JWT_EXPIRATION_MS=86400000        # 24 hours
JWT_REFRESH_EXPIRATION_MS=604800000  # 7 days
```

### application.yml

Key configurations:
- Server port: 8081
- Context path: `/user`
- Flyway migrations enabled
- JPA auditing enabled
- Swagger UI enabled

## Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport

# Run specific test
./gradlew test --tests AuthServiceTest
```

## API Documentation

Access Swagger UI at:
- Local: http://localhost:8081/user/swagger-ui.html
- Via Gateway: http://localhost/user/swagger-ui.html

## Security Features

1. **Password Hashing**: BCrypt with strength 12
2. **JWT Signing**: HMAC-SHA256 with configurable secret
3. **API Key Generation**: Secure random 64-character keys
4. **CORS**: Configured for all origins (customize for production)
5. **Stateless Sessions**: No server-side session storage
6. **Filter Chain**: API Key → JWT → Authorization

## Error Handling

All errors return standardized JSON responses:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable message",
    "path": "/user/api/endpoint",
    "details": {}
  },
  "timestamp": "2025-11-15T10:30:00Z"
}
```

Common error codes:
- `AUTHENTICATION_FAILED` - Invalid credentials
- `DUPLICATE_RESOURCE` - Username/email already exists
- `RESOURCE_NOT_FOUND` - User not found
- `VALIDATION_FAILED` - Invalid request parameters
- `INTERNAL_SERVER_ERROR` - Unexpected error

## Logging

Configured with Logback:
- **INFO** level for application logs
- **DEBUG** level for Spring Security (configurable)
- Logs include request/response details

## Performance

- **Password Hashing**: ~100ms per operation (BCrypt strength 12)
- **JWT Generation**: <5ms
- **JWT Validation**: <5ms
- **API Key Validation**: Single database query

## Monitoring

Health check endpoint provides:
```json
{
  "status": "healthy",
  "service": "user-service",
  "timestamp": "2025-11-15T10:30:00Z"
}
```

## Troubleshooting

### Database Connection Issues

```bash
# Check database is running
docker-compose ps user-db

# View database logs
docker-compose logs user-db

# Connect to database
docker exec -it user-db psql -U user_service_user -d user_service_db
```

### JWT Validation Fails

- Ensure `JWT_SECRET` is at least 32 characters
- Check token hasn't expired
- Verify token format: `Bearer <token>`

### API Key Not Working

- Ensure header is `X-API-KEY` (case-sensitive)
- Verify API key exists in database
- Check user account is active

## Development Tips

1. **Use Swagger**: Test endpoints via Swagger UI before integration
2. **Check Logs**: Enable DEBUG logging for detailed information
3. **Format Code**: Run `./gradlew javaFormat` before committing
4. **Test Locally**: Use `./gradlew bootRun` for quick iteration

## Contributing

1. Follow Palantir Java Format style
2. Write unit tests for new features
3. Update Swagger annotations
4. Add Flyway migrations for schema changes

## License

MIT License - see root LICENSE file

---

**Status**: ✅ Production Ready

For questions or issues, refer to the main project README or architecture documentation.
