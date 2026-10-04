# Parity Report — Java Spring Boot Backend (`backend/`)

**Status: DENO BACKEND REMOVED — `backend/` (Java) is the only backend. Nothing committed.**
Branch: `migrate/backend` · former Deno `backend/` deleted after parity sign-off (all its implemented features verified present; its non-auth modules were 2-line stubs and its `rate_limit`/`redis`/`logger`/`validators` were dead code) · `frontend/` diff is limited to the auth-error display in `AuthForm.tsx` (everything else untouched).

## What was built

Modular monolith in `com.chathub`, feature-first (per CLAUDE.md conventions):

| Layer | Contents |
|---|---|
| `config/` | `AppProperties` (fail-fast env validation), `AppConfig` (BCrypt), `WebConfig` (CORS filter), `WebSocketConfig` (native WS at `/` + origin allow-list), `DatabaseUrlEnvironmentPostProcessor` (`.env` self-load + `DATABASE_URL` parsing) |
| `common/error` | `ApiError`, `GlobalExceptionHandler`, business exceptions |
| `common/security` | `JwtService` (jjwt, HS256, 7d), `JwtAuthFilter` (401 JSON), `AuthenticatedUser` |
| `common/logging` | `JsonLineEncoder`, `RequestLoggingFilter` (token redaction), `LogDetails` |
| `modules/auth/` | controller → service (interface+impl) → repository (interface+JDBC) + DTOs |
| `websocket/` | `SocketRegistry` (interface) + `InMemorySocketRegistry`, `WebSocketAuthFilter` (pre-upgrade `?token=` check), `AuthenticatedUserHandshakeInterceptor`, `ChatWebSocketHandler` |
| migrations | Flyway `V1__create_users.sql` + `baseline-on-migrate` (shared DB compatible) |

Stack: Spring Boot 4.1.1 · Java 21 (toolchain) · Gradle 9.7.1 · JdbcTemplate · jjwt 0.12.6 · spring-security-crypto only (no full Spring Security) · raw `TextWebSocketHandler`.

**36 main + 10 test source files.**

## Validation results

### C1–C14 parity checklist: **17/17 PASS** (live server, port 8000)

| Check | Result |
|---|---|
| C1 signup/login → `200/201 {user{id,username,email}, token}`, no `password_hash` | PASS |
| C2 duplicate email/username → `400` exact messages | PASS |
| C3 valid login → `200 {user, token}` | PASS |
| C4 bad password / unknown email → `400 "Invalid credentials"` | PASS |
| C5 JWT HS256, claims `{id,email,iat,exp}`, 7-day expiry, signature verifies against `JWT_SECRET` | PASS |
| C6 CORS preflight from `http://localhost:5173`: allow-origin echo, methods incl. POST, headers incl. Authorization | PASS |
| C7 no token → `401 {"error":"Missing Authorization header"}` | PASS |
| C8 bad token → `401 {"error":"Invalid or expired token"}` | PASS |
| C9 WS upgrade no token → `401` pre-upgrade (Deno parity) | PASS |
| C10 WS upgrade bad token → `401` pre-upgrade | PASS |
| C11 broadcast `{user, content, timestamp}` to **all** clients; `timestamp` = `YYYY-MM-DDTHH:mm:ss.SSSZ`; missing user → `"Anonymous"` | PASS |
| C12 malformed frame → logged & ignored, connection stays open | PASS |
| C13 listening on port 8000 | PASS |
| C14 bcrypt cost 10 (`$2a$10$`) | PASS |

### Automated tests: **56 tests, 0 failures** (`./gradlew build`, Testcontainers PostgreSQL 15)

| Test class | Tests | Scope |
|---|---|---|
| `JwtServiceTest` | 7 | HS256/claims/7d/signature, expiry, wrong key, tampering, short secret |
| `AuthServiceImplTest` | 6 | Mockito: dup checks, hashing, login failures |
| `ChatWebSocketHandlerTest` | 5 | Mockito: broadcast, Anonymous, malformed, closed clients, registry |
| `AuthApiIntegrationTest` | 12 | MockMvc + real PG: C1–C8, CORS, validation, 404/405 |
| `WebSocketIntegrationTest` | 8 | Live server: handshake 401s, 2-client broadcast, malformed, origin checks (localhost → 101, cross-site → 403, no-Origin → 101) |
| `JsonLineEncoderTest` | 7 | JSON schema, MDC details, root-cause error, bounded stack, multiline flatten |
| `DatabaseUrlEnvironmentPostProcessorTest` | 10 | `.env` load, OS-env precedence, fail-fast messages, jdbc passthrough |
| `ChatHubApplicationTests` | 1 | context load |

### Frontend E2E: **13/13 contract checks PASS**
- `npm run build` (tsc + vite): ✅
- Contract script replicating `auth.store.ts` + `useSocket.ts` exactly (URLs, payloads, WS URL construction, `Message` shape, logout close): 13/13 PASS
- Post-port change (only frontend diff): `AuthForm.tsx` extracts `response.data.error` and displays "Invalid email or password" for `"Invalid credentials"` instead of the generic axios message

### Lint/typecheck notes
- `npm run lint` flags `AuthForm.tsx` `no-explicit-any` on the `catch` line — predates the port (file does carry the auth-error display change above)
- Deno `deno task check` note removed — the Deno backend is deleted

## Deliberate deviations (document, do not "fix" silently)

| # | Deviation | Rationale |
|---|---|---|
| D1 | `password_hash` never returned | Deno leaked it via `RETURNING *`; Java uses a public DTO. Strictly better; frontend `User` type unaffected |
| D2 | Jakarta validation on signup (`@Email`, password 8–72, username ≤32) | Absorbs issue #7; Deno accepted anything. Frontend already sends valid values |
| D3 | CORS preflight returns `200` (Deno `204`) | Both satisfy browsers |
| D4 | 404 body always `{"error":"Not found"}` | Deno returned plain text on some paths; frontend only reads JSON `error` |
| D5 | WS handshake served only at `/` | Deno upgraded any non-auth path; frontend only uses `/` (also prevents accidental `/auth` upgrades) |
| D6 | Non-object JSON frames ignored | Deno broadcast a userless skeleton; frontend always sends `{user, content}` objects |
| D7 | Startup fails fast if `JWT_SECRET`/`DATABASE_URL` missing | Deno used insecure defaults; fail-fast is safer |
| D8 | Secrets <32 bytes are SHA-256-derived for HS256 keys | jjwt requires ≥256-bit keys |
| D9 | WS cross-site `Origin` → `403` (allow-list: `http://localhost:*`, `http://127.0.0.1:*`) | Deno accepted any origin; browser clients get same-origin/localhost, non-browser (no Origin) always pass |
| D10 | Frames without `content` serialize `"content":null`; whitespace-only `user` → `"Anonymous"` | Deno omitted the key / kept whitespace; frontend always sends both fields — wire-invisible in practice |

## JWT secret

Deno is gone, so there is no cross-backend interop caveat anymore. Keep `JWT_SECRET` **≥32 characters** in `backend/.env` anyway (Java SHA-256-derives shorter secrets).

## Shared database state

- Host PG moved to **:5433** (host :5432 owned by Homebrew postgresql@18) — `docker-compose.yml` + `backend/.env` updated
- Flyway `baseline-on-migrate`: shared DB baselined, existing users preserved (Ajoy, AB); fresh DBs (tests) run `V1`
- Test fixture present: `parity_user` / `parity@example.com` / `password123` (temp E2E rows deleted)

## Cutover status

1. **Cutover — DONE**: Deno `backend/` deleted; `docker-compose.yml` is infra-only (postgres + optional frontend container); backend runs natively via `./gradlew bootRun` on :8000
2. **Commit — PENDING**: nothing committed on `migrate/backend`
3. **Docs — DONE**: root `CLAUDE.md` rewritten for the Java backend; `backend/README.md` updated
4. **Issues**: reword/close #7 (validation) and #21 (tests) as absorbed
5. Room/presence/message/task features (#8–#22) remain future work (the Deno stubs for these were deleted with the old backend)

## Reproduce

```bash
docker compose up -d postgres       # PG on :5433
cd backend && cp .env.example .env  # set DATABASE_URL (:5433) + JWT_SECRET (≥32 chars)
./gradlew build                     # 56 tests incl. Testcontainers (Docker required)
./gradlew bootRun                   # http://localhost:8000
```
