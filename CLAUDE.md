# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Chat Hub is a real-time chat application built as a **modular monolith**. It uses native WebSockets (not Socket.IO) intentionally — the project is structured to learn WebSocket fundamentals before adding production abstractions.

## Architecture

```
Frontend (React + Vite)         port 5173
   ↓  HTTP (axios)
   ↓  WebSocket (native)
Backend (Deno)                  port 8000
   ↓
PostgreSQL                      port 5432
Redis                           port 6379
```

### Backend structure (`backend/`)

Entry point: `main.ts` creates a `Set<WebSocket>` (the global clients registry) and passes it to `Deno.serve`. Every request goes through `src/request_handler.ts`:
- `/auth/*` → `src/modules/auth/` (handler → service → repository pattern)
- `Upgrade: websocket` → `src/socket_handler.ts` (onopen/onmessage/onclose handlers)
- Everything else → 404

Layers:
- `src/modules/` — feature modules (auth, chats, rooms, tasks, notifications, users); auth is the only fully implemented one
- `src/websocket/` — placeholders for room_manager, presence_manager, socket_manager, events (not yet implemented)
- `src/middleware/` — JWT auth middleware (defined but not yet wired into routing), rate limit middleware (Redis-backed, placeholder)
- `src/database/postgres.ts` — single `db` client instance
- `src/redis/redis_client.ts` — single `redis` connection instance
- `src/utils/` — jwt (djwt), logger, validators
- `src/config/env.ts` — reads `DATABASE_URL`, `REDIS_URL`, `JWT_SECRET` from environment

### Frontend structure (`frontend/`)

- `src/store/` — Zustand stores (auth.store.ts is implemented; chat.store.ts and task.store.ts are placeholders)
- `src/features/` — feature slices; `auth/AuthForm.tsx` is fully implemented using MUI
- `src/components/` — UI components (ChatPanel, TaskPanel, Sidebar, LoadingSkeleton)
- `src/services/` — api.ts and socket.ts are placeholders; storage.ts exists but is unreferenced
- `src/hooks/` — useAuth.ts and useSocket.ts are placeholders
- `src/App.tsx` — renders `<AuthForm>` when unauthenticated, placeholder chat UI when authenticated
- API base URL: `VITE_API_URL` env var (defaults to `http://localhost:8000`)
- JWT token persisted in `localStorage`, but the auth store does **not** rehydrate from localStorage on page refresh — `user` initializes to `null` even when a token exists

## Commands

### Backend (Deno)

```bash
cd backend
cp .env.example .env          # required — Deno won't start without DATABASE_URL and JWT_SECRET
deno task dev                 # run with --watch (loads .env automatically via --env flag)
deno task check               # type-check without running
deno task fmt                 # format
```

Deno 2 — all dependencies use inline `jsr:` and `npm:` specifiers directly in source files. No import map is active. The `--env` flag in the dev task loads `.env` from the `backend/` directory automatically.

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
docker compose up postgres redis    # start only DB + Redis locally
docker compose up                   # full stack including backend + frontend containers
```

### Database setup

There are no migration files. The database schema must be created manually. The `users` table (required by the auth module) is:

```sql
CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  username TEXT UNIQUE NOT NULL,
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL
);
```

## Key implementation notes

**No test infrastructure.** There are no test files or test runner configuration in either the backend or frontend.

**Adding new HTTP routes** requires extending `request_handler.ts`. Each route module exports an `async function handleXxxRoutes(request: Request): Promise<Response>`. CORS headers are applied centrally by `withCors()` in `request_handler.ts`; new route branches should wrap their handler result with `withCors()`.

**WebSocket broadcasting** broadcasts to all connected clients via the `Set<WebSocket>` passed from `main.ts`. Room-scoped broadcasting is planned in `src/websocket/room_manager.ts` but not yet implemented.

**Many files contain `// ...existing code...` placeholder comments.** These are stubs for planned features, not remnants to be deleted.

**JWT** uses `npm:jose` (HS256, 7-day expiry); tokens are signed/verified in `src/utils/jwt.ts` using a `TextEncoder`-encoded `JWT_SECRET`. Password hashing uses `npm:bcryptjs` (10 salt rounds).

**Auth store** (`frontend/src/store/auth.store.ts`) uses Zustand `persist` middleware — user and token survive page refresh under the `"auth-storage"` localStorage key. Any code that previously read `localStorage.getItem("token")` should read from the store instead.
