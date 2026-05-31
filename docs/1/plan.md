# Plan: authMiddleware wired — all routes protected (#1)

## Objective
Wire JWT authentication into every non-auth HTTP route and the WebSocket upgrade path. After this change, requests missing a valid token receive `401 Unauthorized` before reaching any handler. `POST /auth/signup` and `POST /auth/login` remain public.

## Scope
### In Scope
- Rewrite `backend/src/middleware/auth.ts` to export a native-Deno-HTTP guard: `requireAuth(request: Request): Promise<JWTPayload | Response>`
- Wire `requireAuth` into `request_handler.ts` for all non-auth branches
- WebSocket upgrade path: read token from `?token=<jwt>` query param; reject with `401` if missing/invalid
- Pass verified JWT payload as a second argument to route handlers
- Return `401` JSON errors wrapped in `withCors()` for missing/invalid tokens
- Exempt `POST /auth/signup` and `POST /auth/login`

### Out of Scope
- WebSocket message-layer auth (room/presence)
- Frontend token handling changes
- Role-based access control
- New routes (`/users`, `/rooms`, `/messages`, `/tasks`)

## Approach

Rewrite `authMiddleware` as a pure guard function with no framework coupling:

```ts
// Returns JWT payload on success, or a 401 Response on failure
export async function requireAuth(request: Request): Promise<JWTPayload | Response>
```

In `request_handler.ts`:
1. `/auth/*` — bypass the guard entirely (routes stay public)
2. All other HTTP routes — call `requireAuth(request)`; if the result is a `Response`, return it wrapped in `withCors()`; otherwise pass `result` (the `JWTPayload`) as a second argument to the handler
3. WebSocket upgrade — extract `?token=` from the URL query string, verify it with `verifyJwt`; reject with a `withCors(new Response(..., { status: 401 }))` before the upgrade if missing/invalid; on success pass the payload to socket handlers

## Affected Areas
| Area | Files / Modules | Change Type |
|------|----------------|-------------|
| Auth middleware | `backend/src/middleware/auth.ts` | Modify (rewrite signature) |
| Request handler | `backend/src/request_handler.ts` | Modify (import + apply guard) |

## Assumptions
1. `[ASSUMPTION]` `authMiddleware` export is renamed to `requireAuth` to reflect the new pattern — a guard, not an Oak middleware.
2. `[ASSUMPTION]` WebSocket token is read from `?token=` query param (browser `WebSocket` constructor supports query strings).
3. `[ASSUMPTION]` No test infrastructure introduced.
4. `[ASSUMPTION]` Handler signatures updated to accept an optional `user: JWTPayload` second arg — currently only `handleAuthRoutes` exists and it won't receive the payload (it's exempted); future handlers can use it.

## Open Questions (resolved)
| # | Question | Answer / Decision |
|---|----------|------------------|
| Q1 | WebSocket auth mechanism? | Token in `?token=` query string |
| Q2 | JWT payload forwarding to handlers? | Second argument to handler functions |

## Status: Completed
Implemented in 2 tasks. All type checks passing. Final commit: 3d8bb39.

## Risks & Mitigations
| Risk | Mitigation |
|------|-----------|
| Existing frontend `axios` calls missing `Authorization` header | Frontend Zustand auth store already persists token; verify `api.ts` includes header when making calls |
| Browser WebSocket API doesn't support custom headers | Mitigated by using `?token=` query string instead |
