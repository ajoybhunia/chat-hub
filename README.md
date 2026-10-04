# Chat Hub

A real-time chat application where messages appear instantly for everyone — built as a **modular monolith** using native WebSockets (no Socket.IO), so every layer of the real-time stack stays visible and understandable.

**Current status:** email/password auth and live chat broadcasting work end-to-end. Rooms, presence, message history, tasks, and notifications are planned — see [Roadmap](#roadmap).

## Features

- **Account auth** — signup/login with email + password, JWT sessions (7-day expiry), bcrypt-hashed passwords
- **Live chat** — type a message, every connected user receives it instantly (including you), with a server-assigned UTC timestamp
- **Session persistence** — stay logged in across page refreshes
- **Single-origin BFF** — the browser only talks to its own origin; a Vite plugin proxies `/api/*` and the WebSocket to the backend (no CORS, no extra process)
- **Centralized UI text** — every rendered string lives in `frontend/src/constants/labels.json`
- **Friendly errors** — clear messages like "Invalid email or password" instead of raw HTTP codes
- **Validation** — well-formed email, password length, and username length enforced server-side
- **Structured JSON logging** — one JSON line per request/event, secrets redacted (`token=***`); pretty one-liners in dev
- **Fail-fast config** — the backend refuses to start with missing `DATABASE_URL`/`JWT_SECRET`, with an actionable message

## Tech stack

| Layer | Technology | Port |
|---|---|---|
| Frontend | React 19 + Vite + MUI + Zustand | 5173 |
| BFF | Vite middleware plugin (`frontend/bff/proxy.ts`, same process) | shares 5173 |
| Backend | Java 21 · Spring Boot 4 · Gradle | 8000 |
| Database | PostgreSQL 15 + Flyway migrations | 5433 (host) |
| Realtime | Native WebSocket (`ws://…/socket?token=<jwt>`, piped by the BFF) | — |

*The browser never addresses `:8000`: HTTP goes through `/api/*` and the socket through `/socket`, both handled by the BFF plugin inside the Vite dev/preview process (upstream set by `UPSTREAM_URL`).*

## Getting started

**Prerequisites:** Docker, Node.js 20+. Java 21 is downloaded automatically by Gradle on first run.

**1. Start PostgreSQL**

```bash
docker compose up -d postgres
```

**2. Start the backend**

```bash
cd backend
cp .env.example .env       # then edit DATABASE_URL + JWT_SECRET (≥32 chars)
./gradlew bootRun          # http://localhost:8000
```

**3. Start the frontend**

```bash
cd frontend
cp .env.example .env       # defaults already work
npm install
npm run dev                # http://localhost:5173
```

Open **http://localhost:5173**, create an account, and start chatting — every browser tab you open with a logged-in account sees the same messages.

## Configuration

### Backend — `backend/.env` (loaded automatically on every launch path)

| Variable | Required | Notes |
|---|---|---|
| `DATABASE_URL` | yes | `postgres://user:pass@host:5433/chatdb` or `jdbc:postgresql://…` |
| `JWT_SECRET` | yes | Signing key, **≥32 characters** |

### Frontend — `frontend/.env`

| Variable | Default | Notes |
|---|---|---|
| `UPSTREAM_URL` | `http://localhost:8000` | Backend origin the BFF forwards to — server-side only, never sent to the browser |

## API & WebSocket reference

The browser calls these through the BFF on its own origin (`/api/auth/login`, …); paths below are the backend contract, also reachable directly at `http://localhost:8000` for debugging. All HTTP errors use the shape `{"error": "<message>"}`.

| Method & path | Success | Errors |
|---|---|---|
| `POST /auth/signup` | `201 {user{id,username,email}, token}` | `400` duplicate email/username, validation |
| `POST /auth/login` | `200 {user, token}` | `400 "Invalid credentials"` |
| any other route | — | `401` missing/bad token · `404 Not found` |

**WebSocket:** connect to `ws://localhost:5173/socket?token=<jwt>` — the BFF pipes the upgrade to the backend's `/` endpoint, where the token is verified *before* the upgrade (bad/missing → rejected). If the backend is down, the BFF answers HTTP calls with `502 {"error": …}`.

```json
→ {"user": "Alice", "content": "hello"}
← {"user": "Alice", "content": "hello", "timestamp": "2026-10-04T16:30:00.123Z"}
```

The server fills in `timestamp` and defaults a missing `user` to `"Anonymous"`. Every connected client receives every message. Cross-site browser origins are rejected with `403` (localhost origins allowed).

## Frontend conventions

- **Labels** — UI strings live in `src/constants/labels.json`, re-exported by `src/constants/labels.ts`; components never hardcode rendered text.
- **Services invariant** — only `src/services/*` talks to the server (axios instance with `/api` base + Bearer interceptor; `socket.ts` builds the WS URL); stores orchestrate state, never fetch.
- **Logging** — `log.info(context, message)` from `src/lib/logger.ts`: context first (snake_case keys, errors under `err`), pretty in dev, single-line JSON in prod, `token`/`password` keys redacted.

## Testing

```bash
cd backend && ./gradlew build    # 56 tests — unit, MockMvc, live WebSocket (Docker required for Testcontainers)
cd frontend && npm run build     # type-check + production build (also type-checks bff/)
cd frontend && npm run lint      # eslint
docker-compose build frontend    # optional: containerized dev server
```

## Project structure

```
chat-hub/
├── backend/                 Spring Boot app (Java 21, Gradle wrapper)
│   └── src/main/java/com/chathub/
│       ├── modules/         feature slices: controller → service → repository
│       ├── websocket/       WS auth filter, chat handler, client registry
│       ├── common/          error contract, JWT security, JSON logging
│       └── config/          CORS, WS origins, BCrypt, .env loading
├── frontend/                React + Vite app (BFF lives here too)
│   ├── bff/                 Vite plugin: /api proxy + /socket upgrade pipe
│   └── src/
│       ├── features/        auth, chat, rooms, tasks, notifications
│       ├── components/      ChatPanel, Sidebar, TaskPanel
│       ├── constants/       labels.json (all UI strings), labels.ts, paths.ts
│       ├── lib/             logger.ts (pretty dev / JSON prod, redaction)
│       ├── services/        the only code that talks to the server (api/auth/socket)
│       ├── store/           Zustand stores (auth persists to localStorage)
│       └── hooks/           useSocket (native WebSocket via BFF /socket)
├── docker-compose.yml       PostgreSQL (+ optional frontend container)
└── CLAUDE.md                contributor/agent guidance
```

## Roadmap

- **Rooms** — scoped conversations instead of a single broadcast
- **Presence** — who's online, typing indicators
- **Message history** — persistence + scrollback (messages are broadcast-only today)
- **Tasks & notifications** — the `features/` scaffolds are in place

## History

The original backend was written in Deno/TypeScript and was ported to Java/Spring Boot with full wire-level parity (17/17 contract checks, 56 automated tests); the Deno code has since been removed. Details in [`backend/PARITY_REPORT.md`](backend/PARITY_REPORT.md).
