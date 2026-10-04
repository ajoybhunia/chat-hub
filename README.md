# Chat Hub

A real-time chat application where messages appear instantly for everyone — built as a **modular monolith** using native WebSockets (no Socket.IO), so every layer of the real-time stack stays visible and understandable.

**Current status:** email/password auth and live chat broadcasting work end-to-end. Rooms, presence, message history, tasks, and notifications are planned — see [Roadmap](#roadmap).

## Features

- **Account auth** — signup/login with email + password, JWT sessions (7-day expiry), bcrypt-hashed passwords
- **Live chat** — type a message, every connected user receives it instantly (including you), with a server-assigned UTC timestamp
- **Session persistence** — stay logged in across page refreshes
- **Friendly errors** — clear messages like "Invalid email or password" instead of raw HTTP codes
- **Validation** — well-formed email, password length, and username length enforced server-side
- **Structured JSON logging** — one JSON line per request/event, secrets redacted (`token=***`)
- **Fail-fast config** — the backend refuses to start with missing `DATABASE_URL`/`JWT_SECRET`, with an actionable message

## Tech stack

| Layer | Technology | Port |
|---|---|---|
| Frontend | React 19 + Vite + MUI + Zustand | 5173 |
| Backend | Java 21 · Spring Boot 4 · Gradle | 8000 |
| Database | PostgreSQL 15 + Flyway migrations | 5433 (host) |
| Realtime | Native WebSocket (`ws://…/?token=<jwt>`) | — |

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
| `VITE_API_URL` | `http://localhost:8000` | Backend base URL (HTTP + `ws://` derived from it) |

## API & WebSocket reference

All HTTP errors use the shape `{"error": "<message>"}`.

| Method & path | Success | Errors |
|---|---|---|
| `POST /auth/signup` | `201 {user{id,username,email}, token}` | `400` duplicate email/username, validation |
| `POST /auth/login` | `200 {user, token}` | `400 "Invalid credentials"` |
| any other route | — | `401` missing/bad token · `404 Not found` |

**WebSocket:** connect to `ws://localhost:8000/?token=<jwt>` — the token is verified *before* the upgrade (bad/missing → `401`).

```json
→ {"user": "Alice", "content": "hello"}
← {"user": "Alice", "content": "hello", "timestamp": "2026-10-04T16:30:00.123Z"}
```

The server fills in `timestamp` and defaults a missing `user` to `"Anonymous"`. Every connected client receives every message. Cross-site browser origins are rejected with `403` (localhost origins allowed).

## Testing

```bash
cd backend && ./gradlew build    # 56 tests — unit, MockMvc, live WebSocket (Docker required for Testcontainers)
cd frontend && npm run build     # type-check + production build
cd frontend && npm run lint      # eslint
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
├── frontend/                React + Vite app
│   └── src/
│       ├── features/        auth, chat, rooms, tasks, notifications
│       ├── components/      ChatPanel, Sidebar, TaskPanel
│       ├── store/           Zustand stores (auth persists to localStorage)
│       └── hooks/           useSocket (native WebSocket + ?token=)
├── docker-compose.yml       PostgreSQL (infra only — apps run natively)
└── CLAUDE.md                contributor/agent guidance
```

## Roadmap

- **Rooms** — scoped conversations instead of a single broadcast
- **Presence** — who's online, typing indicators
- **Message history** — persistence + scrollback (messages are broadcast-only today)
- **Tasks & notifications** — the `features/` scaffolds are in place

## History

The original backend was written in Deno/TypeScript and was ported to Java/Spring Boot with full wire-level parity (17/17 contract checks, 56 automated tests); the Deno code has since been removed. Details in [`backend/PARITY_REPORT.md`](backend/PARITY_REPORT.md).
