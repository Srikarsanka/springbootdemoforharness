# D&B Task Manager — Backend Service

A containerized, production-grade REST API backend for enterprise task and project management, built with **Java 21 (LTS)**, **Spring Boot 3.2**, **Spring Security with JWT**, and **MongoDB**.

---

## 1. Architecture & Tech Stack

- **Runtime**: Java 21 (Eclipse Temurin LTS)
- **Framework**: Spring Boot 3.2.5
- **Security**: Spring Security 6 with stateless HMAC-SHA256 JWT tokens & BCrypt password hashing
- **Persistence**: Spring Data MongoDB with automated document auditing (`@CreatedDate`, `@LastModifiedDate`)
- **API Documentation**: SpringDoc OpenAPI 3.0 (Swagger UI at `/swagger-ui.html`)
- **Observability**: Spring Boot Actuator (`/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/metrics`, `/actuator/prometheus`)
- **Build Tool**: Apache Maven 3.9+
- **Containerization**: Multi-stage Docker build producing an unprivileged distroless-style JRE runner

---

## 2. Directory Layout

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/dnb/taskmanager/
│   │   │   ├── TaskManagerApplication.java
│   │   │   ├── config/             # MongoConfig, SecurityConfig, WebConfig, OpenApiConfig
│   │   │   ├── controller/         # Auth, User, Project, Task, Comment, Health controllers
│   │   │   ├── dto/                # Request records & Response records
│   │   │   ├── exception/          # GlobalExceptionHandler, Custom exceptions
│   │   │   ├── model/              # MongoDB Documents & Domain Enums
│   │   │   ├── repository/         # Spring Data Mongo Repositories
│   │   │   ├── security/           # JWT Provider, Auth Filter, UserDetailsService
│   │   │   └── service/            # Core business domain services
│   │   └── resources/
│   │       ├── application.yml      # Base default configuration
│   │       ├── application-dev.yml  # Local developer settings
│   │       ├── application-qa.yml   # QA environment profile
│   │       ├── application-stg.yml  # Staging environment profile
│   │       └── application-prod.yml # Production profile
│   └── test/
│       ├── java/com/dnb/taskmanager/
│       │   ├── controller/         # MockMvc integration tests
│       │   ├── security/           # JWT generation & validation tests
│       │   └── service/            # Business unit tests with Mockito
│       └── resources/
│           └── application-test.yml
├── Dockerfile                      # Multi-stage Java 21 container image
├── pom.xml                         # Project Object Model
├── settings.xml.example            # Example enterprise artifact mirror settings
├── .env.example                    # Environment variable documentation
└── .gitignore
```

---

## 3. Environment Variables & Runtime Contract

The application is completely externalized and configured through environment variables. This contract enables independent infrastructure provisioning and configuration management:

| Variable | Description | Default | Allowed Values |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` | `dev`, `qa`, `stg`, `prod`, `test` |
| `SERVER_PORT` | HTTP server listening port | `8080` | Valid TCP port (e.g., `8080`) |
| `MONGODB_URI` | MongoDB connection URI string | `mongodb://localhost:27017/taskmanager` | Connection string |
| `JWT_SECRET` | HMAC-SHA256 signing key (min 256 bits) | Dev default (change in prod!) | Random 32+ character string |
| `JWT_EXPIRATION_MS` | JWT expiration duration in milliseconds | `86400000` (24h) | Positive integer |
| `CORS_ALLOWED_ORIGINS` | Comma-delimited list of allowed web origins | `http://localhost:4200` | URL string(s) |
| `JAVA_OPTS` | JVM memory, GC, and container flags | `-XX:+UseContainerSupport...` | Standard JVM flags |

---

## 4. API Endpoints Catalog

### Authentication (`/api/auth`)
- `POST /api/auth/register` — Register a new account (`email`, `firstName`, `lastName`, `password`, `role`)
- `POST /api/auth/login` — Authenticate and retrieve JWT token

### Users (`/api/users`)
- `GET /api/users` — List all users (authenticated)
- `GET /api/users/me` — Current authenticated user profile
- `GET /api/users/{id}` — Retrieve user by ID
- `PUT /api/users/{id}` — Update user profile
- `DELETE /api/users/{id}` — Delete user (requires `ROLE_ADMIN`)

### Projects (`/api/projects`)
- `GET /api/projects` — List accessible projects for current user
- `POST /api/projects` — Create project
- `GET /api/projects/{id}` — Retrieve project details
- `PUT /api/projects/{id}` — Update project (owner or admin)
- `DELETE /api/projects/{id}` — Delete project (owner or admin)
- `POST /api/projects/{id}/members?memberId={id}` — Add team member
- `DELETE /api/projects/{id}/members/{memberId}` — Remove team member

### Tasks & Kanban (`/api/tasks`)
- `GET /api/tasks/project/{projectId}` — List all tasks in a project
- `GET /api/tasks/my-tasks` — List tasks assigned to current user
- `POST /api/tasks` — Create new task
- `GET /api/tasks/{id}` — Get task details
- `PUT /api/tasks/{id}` — Update full task
- `PATCH /api/tasks/{id}/status?status={STATUS}` — Update task status (Kanban column transition)
- `DELETE /api/tasks/{id}` — Delete task

### Comments (`/api`)
- `GET /api/tasks/{taskId}/comments` — Get task comments
- `POST /api/tasks/{taskId}/comments` — Add comment to task
- `DELETE /api/comments/{id}` — Delete comment (author or admin)

### Health & Observability
- `GET /api/health` — Application-level status and MongoDB ping verification
- `GET /actuator/health` — Spring Boot Actuator composite healthcheck
- `GET /actuator/health/liveness` — Internal JVM and core container liveness probe
- `GET /actuator/health/readiness` — MongoDB and downstream readiness probe

---

## 5. Building and Running

### Prerequisites
- JDK 21+
- Apache Maven 3.9+
- Running MongoDB instance (or Docker)

### Run Locally (Dev Profile)
```bash
# 1. Start MongoDB on localhost:27017
# 2. Run with Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will be available at:
- REST API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Healthcheck: `http://localhost:8080/api/health`

### Build Executable JAR
```bash
mvn clean package -DskipTests
java -jar target/task-manager-1.0.0-SNAPSHOT.jar
```

### Run Automated Tests
```bash
mvn test
```

---

## 6. Docker Containerization

The backend utilizes an optimized multi-stage `Dockerfile`:
- Stage 1 compiles the application using `eclipse-temurin:21-jdk-jammy`.
- Stage 2 copies the binary into a lightweight `eclipse-temurin:21-jre-jammy` runtime image.
- Runs as an unprivileged system user (`appuser:appgroup`, UID 10001).
- Includes built-in `HEALTHCHECK` checking `/actuator/health`.

### Build Container Image
```bash
docker build -t dnb-taskmanager-backend:1.0.0 .
```

### Run Container
```bash
docker run -d \
  --name taskmanager-backend \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e MONGODB_URI="mongodb://host.docker.internal:27017/taskmanager_dev" \
  -e JWT_SECRET="yourStrong256BitOrGreaterKeyHere" \
  -e CORS_ALLOWED_ORIGINS="http://localhost:4200" \
  dnb-taskmanager-backend:1.0.0
```

---

## 7. Future Integration Notes (Liveness & Readiness Probes)

When designing orchestration or hosting platforms:
- **Liveness Probe**: Point to `GET /actuator/health/liveness` (Port 8080). Returns HTTP 200 `{"status": "UP"}` when the process is alive.
- **Readiness Probe**: Point to `GET /actuator/health/readiness` (Port 8080). Returns HTTP 200 only when MongoDB connectivity is established.
- **Graceful Shutdown**: The server has `server.shutdown=graceful` enabled to drain in-flight connections upon receiving `SIGTERM`.

