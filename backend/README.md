# backend

Spring Boot backend for Chat Hub — the only backend (replaces the former Deno `backend/`, removed after a verified full-parity port; see `PARITY_REPORT.md`).

## Stack

- Java 21 (toolchain), Gradle 9.7.1 (wrapper), Spring Boot 4.1.1
- PostgreSQL + Flyway, `JdbcTemplate` persistence, raw WebSocket (no STOMP)
- JWT (jjwt, HS256) + BCrypt via `spring-security-crypto` — no full Spring Security

## Commands

```bash
./gradlew build        # compile + run all tests
./gradlew test         # tests only
./gradlew bootRun      # run on port 8000
```

First run auto-downloads the Gradle distribution and a Java 21 toolchain (foojay).

## Environment variables

| Variable | Required | Purpose |
|---|---|---|
| `DATABASE_URL` | yes | Postgres URL — `postgres://user:pass@host:5432/db` or `jdbc:postgresql://host:5432/db` |
| `JWT_SECRET` | yes | HS256 signing secret (≥32 chars) |

`.env` in this directory is loaded automatically for every launch path (bootRun, IDE, `java -jar`); OS environment variables always win.

## Tests

- Unit/slice tests: Mockito
- `@SpringBootTest` context + repository tests: Testcontainers (`postgres:15`, Docker required)

## Structure

```
src/main/java/com/chathub/
├── config/      cross-cutting wiring (CORS, WebSocket, datasource, .env loading)
├── common/      error handling, security (JWT), JSON logging, shared utils
├── modules/     feature modules: controller → service → repository + dto
└── websocket/   transport layer (handlers, registry, interceptors)
```
