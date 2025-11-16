# Coffee Chat Backend - Multi-Module Gradle Project

This is a multi-module Gradle project containing both the User Service and Chat Service for the Coffee Chat Platform.

## Project Structure

```
backend/
├── build.gradle           # Parent build configuration
├── settings.gradle        # Module definitions
├── gradle.properties      # Gradle settings
├── gradlew               # Gradle wrapper (Unix/Mac)
├── gradlew.bat           # Gradle wrapper (Windows)
├── gradle/               # Gradle wrapper files
├── user-service/
│   ├── build.gradle      # User service specific config
│   ├── src/
│   └── README.md
└── chat-service/
    ├── build.gradle      # Chat service specific config
    └── src/
```

## Benefits of Multi-Module Setup

1. **Shared Configuration**: Common dependencies defined once in parent `build.gradle`
2. **Consistent Versioning**: Both services use the same version number
3. **Easier Builds**: Build both services with a single command
4. **Better IDE Support**: IntelliJ IDEA and Eclipse recognize the structure
5. **Code Reuse**: Can add a `common` module for shared utilities

## Prerequisites

- Java 21
- Gradle 8.5+ (or use the wrapper)
- Docker (for image building)

## Getting Started

### 1. Build All Services

```bash
# From backend directory
./gradlew clean build
```

### 2. Build Specific Service

```bash
# User service only
./gradlew :user-service:build

# Chat service only
./gradlew :chat-service:build
```

### 3. Run Services Locally

```bash
# Run user service
./gradlew :user-service:bootRun

# Run chat service (in another terminal)
./gradlew :chat-service:bootRun
```

## Docker Image Building

### Build All Service Images

```bash
./gradlew jibDockerBuild
```

### Build Specific Service Image

```bash
# User service
./gradlew :user-service:jibDockerBuild

# Chat service
./gradlew :chat-service:jibDockerBuild
```

### Verify Images

```bash
docker images | grep ragchat
```

Expected output:
```
ragchat/user-service    latest    ...
ragchat/chat-service    latest    ...
```

## Code Formatting

The project uses Spotless with google-java-format for consistent backend code style.

### Format All Code

```bash
./gradlew spotlessApply
```

### Check Formatting

```bash
./gradlew spotlessCheck
```

## Testing

### Run All Tests

```bash
./gradlew test
```

### Run Tests for Specific Service

```bash
./gradlew :user-service:test
./gradlew :chat-service:test
```

### Run Tests with Coverage

```bash
./gradlew test jacocoTestReport
```

## Common Gradle Commands

### Build Commands

| Command | Description |
|---------|-------------|
| `./gradlew build` | Build all services |
| `./gradlew clean build` | Clean and build all |
| `./gradlew build -x test` | Build without tests |
| `./gradlew :user-service:build` | Build user service only |
| `./gradlew :chat-service:build` | Build chat service only |

### Run Commands

| Command | Description |
|---------|-------------|
| `./gradlew :user-service:bootRun` | Run user service |
| `./gradlew :chat-service:bootRun` | Run chat service |

### Docker Commands

| Command | Description |
|---------|-------------|
| `./gradlew jibDockerBuild` | Build all Docker images |
| `./gradlew :user-service:jibDockerBuild` | Build user service image |
| `./gradlew :chat-service:jibDockerBuild` | Build chat service image |

### Code Quality Commands

| Command | Description |
|---------|-------------|
| `./gradlew spotlessApply` | Format all backend code |
| `./gradlew spotlessCheck` | Check formatting |
| `./gradlew test` | Run all tests |
| `./gradlew dependencies` | Show dependencies |

### Utility Commands

| Command | Description |
|---------|-------------|
| `./gradlew tasks` | List all available tasks |
| `./gradlew projects` | List all sub-projects |
| `./gradlew --refresh-dependencies` | Refresh dependencies |

## IDE Setup

### IntelliJ IDEA

1. Open IntelliJ IDEA
2. File → Open
3. Select the `backend` directory
4. IntelliJ will automatically detect the Gradle project
5. Wait for indexing to complete

**Recommended Plugins:**
- Lombok (for annotation processing)

**Settings:**
- File → Settings → Tools → Palantir Java Format → Enable
- File → Settings → Build → Build Tools → Gradle → Use Gradle from: 'gradle-wrapper.properties'

### VS Code

1. Open VS Code
2. File → Open Folder
3. Select the `backend` directory
4. Install recommended extensions:
   - Language Support for Java
   - Gradle for Java
   - Lombok Annotations Support

## Project Configuration

### Parent build.gradle

The parent `build.gradle` defines:
- Common plugins (Spring Boot, Spotless, Jib)
- Shared dependencies (Spring, PostgreSQL, JWT, etc.)
- Java version (21)
- Common test configuration

### Service-Specific build.gradle

Each service has its own `build.gradle` for:
- Service-specific dependencies
- Jib Docker configuration (ports, labels)
- Custom tasks

## Environment Variables

Services require these environment variables (set in `.env` or IDE run configurations):

**User Service:**
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/user_service_db
SPRING_DATASOURCE_USERNAME=user_service_user
SPRING_DATASOURCE_PASSWORD=secure_password_1
JWT_SECRET=your-256-bit-secret-key-min-32-chars
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000
```

**Chat Service:**
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/chat_service_db
SPRING_DATASOURCE_USERNAME=chat_service_user
SPRING_DATASOURCE_PASSWORD=secure_password_2
USER_SERVICE_URL=http://localhost:8081/user
JWT_SECRET=your-256-bit-secret-key-min-32-chars
RATE_LIMIT_CAPACITY=100
```

## LLM / Ollama Setup

The chat service uses Spring AI with Ollama. Key environment variables (also defined in `backend/.env`):

- `OLLAMA_MODEL` – the model to use (default: `gemma3:1b`)
- `OLLAMA_HOST` – base URL for the Ollama server, used by `spring.ai.ollama.base-url` in `chat-service`.

Both setups below require the model to be pulled via:

```bash
ollama pull ${OLLAMA_MODEL}
```

### Option 1: Local Ollama (recommended on macOS)

On macOS, Docker Desktop does **not** support GPU passthrough to Linux containers. For faster responses and to take advantage of any local acceleration, it is better to run Ollama directly on the host and call it from the containers via `host.docker.internal`.

1. Install the Ollama CLI on your host machine.
2. Pull the configured model on the host:
    ```bash
    export OLLAMA_MODEL=gemma3:1b   # or your chosen model
    ollama pull ${OLLAMA_MODEL}
    ```
3. Ensure Ollama is running (it listens on `localhost:11434` by default).
4. In `backend/.env`, configure:
    ```bash
    OLLAMA_MODEL=gemma3:1b
    OLLAMA_HOST=http://host.docker.internal:11434
    ```

When you run the backend with Docker Compose, the chat service will reach the Ollama server running on your macOS host via `host.docker.internal`.

### Option 2: Docker `llm` service (for non-macOS or fully containerized setups)

The `backend/docker-compose.yml` file defines an `llm` service based on the `ollama/ollama:latest` image:

- Internal port: `11434`
- Exposed host port: `11435` (mapped as `11435:11434`)

For container-to-container communication, configure `backend/.env` with:

```bash
OLLAMA_MODEL=gemma3:1b
OLLAMA_HOST=http://llm:11434
```

Then ensure the model is available inside the `llm` container:

```bash
# After docker-compose is up
docker exec -it llm ollama pull ${OLLAMA_MODEL}
```

This approach is suitable for environments where you cannot or do not want to install Ollama on the host directly, or where GPU passthrough is not a concern.

## Troubleshooting

### Issue: Gradle wrapper not found

**Solution:**
```bash
gradle wrapper --gradle-version 8.5
```

### Issue: Tests fail with database connection errors

**Solution:** Ensure PostgreSQL is running or use TestContainers (already configured).

### Issue: Jib build fails

**Solution:** Ensure Docker daemon is running:
```bash
docker ps
```

### Issue: IDE doesn't recognize project structure

**Solution:** 
- IntelliJ: File → Invalidate Caches → Restart
- VS Code: Reload window (Cmd/Ctrl + Shift + P → "Reload Window")

## Adding a New Module

To add a new shared module (e.g., `common`):

1. Create directory: `mkdir common`
2. Add to `settings.gradle`: `include 'common'`
3. Create `common/build.gradle`:
```gradle
// No Spring Boot plugin needed for library modules
plugins {
    id 'java-library'
}
```
4. Use in services:
```gradle
dependencies {
    implementation project(':common')
}
```

## Continuous Integration

### GitHub Actions Example

```yaml
name: Backend Build

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Setup Gradle
        uses: gradle/gradle-build-action@v2
      
      - name: Build with Gradle
        run: |
          cd backend
          ./gradlew build
      
      - name: Build Docker Images
        run: |
          cd backend
          ./gradlew jibDockerBuild
```

## Performance Tips

1. **Enable Gradle Daemon**: Already enabled in `gradle.properties`
2. **Use Build Cache**: Already enabled in `gradle.properties`
3. **Parallel Builds**: Already enabled in `gradle.properties`
4. **Incremental Builds**: Gradle automatically detects changes

## Migration from Separate Projects

If migrating from separate Gradle projects:

1. Move `src/` directories to respective service folders
2. Remove service-specific `settings.gradle` files
3. Simplify service `build.gradle` files (remove common config)
4. Run `./gradlew clean build` to verify

See `RESTRUCTURE_GUIDE.md` in the project root for detailed migration steps.

## Contributing

1. Create a feature branch
2. Make changes
3. Run `./gradlew javaFormat` before committing
4. Run `./gradlew test` to ensure tests pass
5. Submit pull request

## License

MIT License - see root LICENSE file for details

---

For service-specific documentation, see:
- [User Service README](user-service/README.md)
- [Chat Service README](chat-service/README.md)
