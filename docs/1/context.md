# Context Summary: Issue #1 — [Bug] authMiddleware defined but not wired — all routes are publicly accessible

## Issue
- **State:** open
- **Labels:** bug, backend
- **Milestone:** none

## What the issue asks for
The `authMiddleware` in `backend/src/middleware/auth.ts` is defined but never called. Every HTTP route is fully public. The fix must wire the middleware into `request_handler.ts` so that all routes except `POST /auth/signup` and `POST /auth/login` require a valid JWT `Authorization: Bearer <token>` header, returning `401` for missing or invalid tokens.

## Linked issues
*(none)*

## Images
*(none)*

## Codebase findings

| Area | Detail |
|---|---|
| Repo structure | Deno backend at `backend/`, React frontend at `frontend/`, no monorepo tooling |
| Relevant files | `backend/src/middleware/auth.ts` — existing but broken middleware; `backend/src/request_handler.ts` — routing entry point to modify; `backend/src/utils/jwt.ts` — `verifyJwt()` used by middleware |
| Test framework | **No tests exist.** No test files found, no test runner configured in `deno.json` |
| Tech stack | Deno 2 + native HTTP (`Request`/`Response`), `npm:jose` for JWT, `npm:bcryptjs` for passwords, React + Vite frontend |
| Relevant docs | CLAUDE.md explains the handler/service/repository pattern and that CORS is applied centrally via `withCors()` |

## Initial observations

**Critical mismatch — Oak API vs native Deno HTTP:** The existing `authMiddleware` uses an Oak-style signature `(ctx, next)` with `ctx.request.headers`, `ctx.response.status`, `ctx.state.user`, and `await next()`. However, the project uses **native Deno HTTP** — no Oak framework is installed. `request_handler.ts` works with plain `Request` objects and returns `Promise<Response>`. The middleware must be rewritten, not just imported, to match this pattern.

**Suggested native-HTTP guard pattern:**
```ts
// Returns user payload to pass through, or a Response to reject
async function requireAuth(request: Request): Promise<JWTPayload | Response>
```

**Public routes to exempt:** only `POST /auth/signup` and `POST /auth/login` (already matched inside `handleAuthRoutes`). The WebSocket upgrade path and any future routes should be protected.

**CORS:** `withCors()` must still wrap the 401 responses so the frontend receives them correctly.
