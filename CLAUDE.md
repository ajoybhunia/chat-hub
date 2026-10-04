# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Chat Hub is a real-time chat application built as a **modular monolith**. It uses native WebSockets (not Socket.IO) intentionally — the project is structured to learn WebSocket fundamentals before adding production abstractions. The backend is **Java/Spring Boot** (`backend/`); the former Deno backend was removed after a verified full-parity port (see `backend/PARITY_REPORT.md`).

## Architecture

```
Frontend (React + Vite)         port 5173
   ↓  HTTP (axios)
   ↓  WebSocket (native)
Backend (Spring Boot, Java)     port 8000
   ↓
PostgreSQL                      port 5433 (host; 5432 in-container)
```

### Backend structure (`backend/`)

Entry point: `ChatHubApplication.java`. All HTTP goes through Spring MVC — there is no hand-rolled request router:

- `config/WebConfig.java` — CORS filter (highest precedence, applied to every response)
- `common/security/JwtAuthFilter.java` — `401` guard on every non-`/auth` request (`Authorization: Bearer`)
- `modules/auth/controller/AuthController.java` — `POST /auth/signup` (201), `POST /auth/login` (200)
- `websocket/WebSocketAuthFilter.java` — validates `?token=` **before** the WS upgrade (401 pre-upgrade)
- Anything unmatched → `404 {"error":"Not found"}` via `common/error/GlobalExceptionHandler.java`

Layers:
- `src/main/java/com/chathub/modules/` — feature modules (`controller → service → repository + dto`); auth is the only implemented one — chats/rooms/tasks/notifications/users are future modules
- `websocket/` — `ChatWebSocketHandler` (broadcast to all clients), `SocketRegistry`/`InMemorySocketRegistry`, `AuthenticatedUserHandshakeInterceptor`
- `common/` — `error/` (ApiError + GlobalExceptionHandler + business exceptions), `security/` (JwtService, JwtAuthFilter, AuthenticatedUser), `logging/` (JsonLineEncoder, RequestLoggingFilter, LogDetails)
- `config/` — WebConfig (CORS), WebSocketConfig (endpoint `/` + origin allow-list), AppConfig (BCrypt), AppProperties, DatabaseUrls + DatabaseUrlEnvironmentPostProcessor (`.env` self-load, fail-fast)
- `src/main/resources/` — `application.yml`, `logback-spring.xml` (JSON logs), Flyway `db/migration/V1__create_users.sql`

### Frontend structure (`frontend/`)

- `src/store/` — Zustand stores (auth.store.ts is implemented; chat.store.ts and task.store.ts are placeholders)
- `src/features/` — feature slices; `auth/AuthForm.tsx` is fully implemented using MUI
- `src/components/` — UI components (ChatPanel, TaskPanel, Sidebar, LoadingSkeleton)
- `src/services/` — api.ts and socket.ts are placeholders; storage.ts exists but is unreferenced
- `src/hooks/` — useAuth.ts is a placeholder; useSocket.ts is implemented (native WS, connects with `?token=`)
- `src/App.tsx` — renders `<AuthForm>` when unauthenticated, placeholder chat UI when authenticated
- API base URL: `VITE_API_URL` env var (defaults to `http://localhost:8000`)
- JWT token persisted in `localStorage`, but the auth store does **not** rehydrate from localStorage on page refresh — `user` initializes to `null` even when a token exists

## Commands

### Backend (Java)

```bash
cd backend
cp .env.example .env          # required — DATABASE_URL + JWT_SECRET (fail-fast at startup)
./gradlew bootRun             # run on port 8000 (.env loaded automatically, every launch path)
./gradlew build               # compile + run all tests (Testcontainers needs Docker)
./gradlew test                # tests only
```

Gradle 9.7.1 wrapper, Java 21 toolchain (auto-downloaded via foojay on first run). No fmt/check tasks — `./gradlew build` is the gate.

### Frontend (npm)

```bash
cd frontend
npm install
cp .env.example .env          # first time setup
npm run dev                   # Vite dev server
npm run build                 # tsc + vite build
npm run lint                  # eslint
```

### Infrastructure

```bash
docker compose up postgres    # start only the DB locally (host port 5433)
```

The backend and frontend run natively (commands above) — compose provides PostgreSQL only; Redis was removed (never used by the Java backend).

### Database setup

Schema is managed by Flyway (`backend/src/main/resources/db/migration/`). `V1__create_users.sql` creates the `users` table (+ email/username indexes), ported from the Deno backend. No manual steps: on an existing DB without Flyway history, `baseline-on-migrate: true` baselines instead of re-running V1.

## Key implementation notes

**Backend tests exist:** 56 tests (unit + Mockito, MockMvc + Testcontainers PostgreSQL 15, live WebSocket integration). Run them with `./gradlew build` — Docker must be running for Testcontainers. The frontend has no test runner; its gate is `npm run build` + `npm run lint`.

**Adding new HTTP routes:** create a `@RestController` under `modules/<feature>/controller/` — no central router to edit. CORS is applied centrally by the `WebConfig` CorsFilter; error bodies (`{"error": message}`) come from `GlobalExceptionHandler` — throw the existing business exceptions (`DuplicateResourceException`, `InvalidCredentialsException`) or add an `@ExceptionHandler` there.

**WebSocket broadcasting** goes to all connected clients through the `SocketRegistry` (`InMemorySocketRegistry`). The handshake is served only at `/` (`WebSocketConfig`), browser origins are checked against `app.websocket-allowed-origin-patterns` in `application.yml` (cross-site → 403; same-origin and no-Origin clients always pass), and `?token=` is verified pre-upgrade by `WebSocketAuthFilter`.

**JWT** uses jjwt (HS256, 7-day expiry) in `common/security/JwtService.java`; secrets shorter than 32 bytes are SHA-256-derived. `JWT_SECRET` should be ≥32 chars. Password hashing is BCrypt cost 10 (`config/AppConfig.java`).

**Configuration** is read by `DatabaseUrlEnvironmentPostProcessor`: `backend/.env` is loaded automatically for every launch path (bootRun, IDE, `java -jar`); `DATABASE_URL` and `JWT_SECRET` fail startup fast when missing. `DATABASE_URL` accepts both `postgres://user:pass@host:5432/db` and `jdbc:postgresql://...` forms.

**Logging** is JSON lines to the console (`logback-spring.xml` + `JsonLineEncoder`). Request logs redact `token=` to `***`; structured fields are attached with `LogDetails.with(...)`. Errors log a concise root-cause message with a bounded stack trace.

**Auth store** (`frontend/src/store/auth.store.ts`) uses Zustand `persist` middleware — user and token survive page refresh under the `"auth-storage"` localStorage key. Any code that previously read `localStorage.getItem("token")` should read from the store instead.
