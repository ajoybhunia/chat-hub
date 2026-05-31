# Tasks: Issue #1 — authMiddleware wired — all routes protected

## Task 1: Rewrite `requireAuth` guard in `auth.ts`

**Description**
Replace the Oak-style `authMiddleware(ctx, next)` with a native-Deno-HTTP guard function that accepts a `Request` and returns either a verified `JWTPayload` (pass) or a `401 Response` (reject). Rename the export from `authMiddleware` to `requireAuth`.

**Acceptance Criteria**
- [ ] `requireAuth(request)` returns `JWTPayload` when `Authorization: Bearer <valid-token>` is present
- [ ] Returns `Response(401, { error: "Missing Authorization header" })` when header is absent
- [ ] Returns `Response(401, { error: "Invalid or expired token" })` when token is expired or malformed
- [ ] No Oak imports or `ctx`/`next` references remain in the file
- [ ] `verifyJwt` from `../utils/jwt.ts` is still used for verification

**Files Likely Affected**
- `backend/src/middleware/auth.ts` — full rewrite

**Test Requirements**
- No test infrastructure exists in this repo; manual verification via `deno task dev` + curl
- Edge cases to cover mentally: missing header, malformed token, expired token, valid token

**Dependencies**
- None (standalone)

**Estimated Complexity**
S

---

## Task 2: Wire `requireAuth` into `request_handler.ts`

**Description**
Import `requireAuth` and apply it in `request_handler.ts` for all non-auth branches: (a) future HTTP routes fallback and (b) the WebSocket upgrade path. For HTTP routes, if `requireAuth` returns a `Response`, return it wrapped in `withCors()`; otherwise pass the `JWTPayload` as a second argument. For WebSocket, read `?token=` from the URL query string, verify with `verifyJwt` directly, and reject before the upgrade if missing/invalid.

**Acceptance Criteria**
- [ ] `requireAuth` is imported from `./middleware/auth.ts`
- [ ] Non-auth HTTP requests without a valid token → `401 { error: "Missing Authorization header" }` with CORS headers
- [ ] Non-auth HTTP requests with invalid/expired token → `401 { error: "Invalid or expired token" }` with CORS headers
- [ ] Non-auth HTTP requests with valid token → request proceeds; `JWTPayload` passed as second arg to handler
- [ ] WebSocket upgrade without `?token=` → `401 { error: "Missing Authorization header" }` (no upgrade)
- [ ] WebSocket upgrade with invalid `?token=` → `401 { error: "Invalid or expired token" }` (no upgrade)
- [ ] WebSocket upgrade with valid `?token=` → upgrade proceeds normally
- [ ] `POST /auth/signup` and `POST /auth/login` remain fully accessible without a token

**Files Likely Affected**
- `backend/src/request_handler.ts` — import + apply guard in two branches

**Test Requirements**
- Manual verification via curl (HTTP) and wscat/browser console (WebSocket)
- No automated tests (no infrastructure)

**Dependencies**
- Depends on Task 1

**Estimated Complexity**
M
