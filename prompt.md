# REAL-TIME CHAT + TASK MANAGEMENT APP

Build a full-stack real-time chat application with collaborative task management
features layered on top of chat conversations.

The application is intended for learning, experimentation, and AI-assisted vibe
coding — not enterprise production use.

The existing project already contains a minimal backend and frontend setup.\
Expand the current structure incrementally instead of rebuilding everything from
scratch.

---

# Existing Project Structure

Use and evolve the following structure instead of replacing it:

```txt
.
├── backend
│   ├── deno.json
│   ├── main.ts
│   └── src
│       ├── request_handler.ts
│       └── socket_handler.ts
├── frontend
│   ├── eslint.config.js
│   ├── index.html
│   ├── package-lock.json
│   ├── package.json
│   ├── public
│   │   ├── favicon.svg
│   │   └── icons.svg
│   ├── README.md
│   ├── src
│   │   ├── App.tsx
│   │   └── main.tsx
│   ├── tsconfig.app.json
│   ├── tsconfig.json
│   ├── tsconfig.node.json
│   └── vite.config.ts
├── GUIDE.md
├── prompt.md
└── README.md
```

Do not restructure aggressively.

Prefer incremental expansion and maintain simplicity.

---

# Goals

Users should be able to:

- Sign up and authenticate
- Chat privately or in public rooms
- Create tasks inside chats
- Assign tasks to users
- Track task progress in real time
- Receive live updates through WebSockets
- View message history and task history
- See user presence and typing indicators

---

# Tech Stack

## Frontend

- React
- Vite
- TypeScript
- Material UI
- Zustand preferred for state management
- React Query optional
- Optimistic UI updates

## Backend

- Deno.js
- TypeScript
- REST API + WebSocket server
- JWT authentication
- Modular architecture without overengineering

## Database

### PostgreSQL

Use PostgreSQL for:

- users
- messages
- chat rooms
- memberships
- tasks
- task comments
- read receipts
- notifications

### Redis

Use Redis for:

- online presence
- typing indicators
- pub/sub broadcasting
- temporary socket state
- rate limiting
- caching

---

# Architecture Rules

Keep the architecture simple and beginner/intermediate friendly.

Avoid:

- microservices
- Kubernetes
- event sourcing
- unnecessary abstraction layers

Use:

- feature-based organization
- small reusable utilities
- clean TypeScript types
- modular handlers/services

---

# Updated Backend Structure

Expand the backend gradually into something like:

```txt
backend/
├── deno.json
├── main.ts
├── .env
└── src/
    ├── config/
    │   ├── env.ts
    │   └── database.ts
    │
    ├── middleware/
    │   ├── auth.ts
    │   └── rate_limit.ts
    │
    ├── modules/
    │   ├── auth/
    │   │   ├── auth_handler.ts
    │   │   ├── auth_service.ts
    │   │   └── auth_repository.ts
    │   │
    │   ├── users/
    │   ├── chats/
    │   ├── rooms/
    │   ├── tasks/
    │   └── notifications/
    │
    ├── websocket/
    │   ├── events.ts
    │   ├── socket_manager.ts
    │   ├── room_manager.ts
    │   └── presence_manager.ts
    │
    ├── redis/
    │   └── redis_client.ts
    │
    ├── database/
    │   ├── postgres.ts
    │   └── migrations/
    │
    ├── utils/
    │   ├── jwt.ts
    │   ├── logger.ts
    │   └── validators.ts
    │
    ├── request_handler.ts
    └── socket_handler.ts
```

---

# Updated Frontend Structure

Expand the frontend gradually into something like:

```txt
frontend/
├── src/
│   ├── components/
│   │   ├── layout/
│   │   ├── chat/
│   │   ├── tasks/
│   │   └── common/
│   │
│   ├── features/
│   │   ├── auth/
│   │   ├── chat/
│   │   ├── rooms/
│   │   ├── tasks/
│   │   └── notifications/
│   │
│   ├── hooks/
│   │   ├── useSocket.ts
│   │   └── useAuth.ts
│   │
│   ├── services/
│   │   ├── api.ts
│   │   ├── socket.ts
│   │   └── storage.ts
│   │
│   ├── store/
│   │   ├── auth.store.ts
│   │   ├── chat.store.ts
│   │   └── task.store.ts
│   │
│   ├── types/
│   │   ├── chat.ts
│   │   ├── task.ts
│   │   └── user.ts
│   │
│   ├── App.tsx
│   └── main.tsx
```

---

# Core Features

## Authentication

Implement:

- signup/login
- JWT authentication
- password hashing
- protected APIs
- protected WebSocket connections

---

## Real-Time Chat

### Private Messaging

Support:

- one-to-one chats
- message persistence
- delivery status
- read receipts
- typing indicators

### Public Rooms

Support:

- public chat rooms
- room joins/leaves
- room broadcasts
- member tracking

---

# Task Management System

Tasks should exist inside chats or rooms.

Each task should support:

- title
- description
- status
- priority
- due date
- creator
- assignee
- comments
- timestamps

Task statuses:

```txt
TODO
IN_PROGRESS
BLOCKED
DONE
```

---

# WebSocket Requirements

Use WebSockets for all live events.

Example events:

```ts
chat:message.send
chat:message.receive
chat:typing.start
chat:typing.stop
presence:update
task:create
task:update
task:delete
task:assign
notification:new
```

Implement:

- reconnect logic
- heartbeat/ping-pong
- exponential backoff
- event acknowledgment
- room subscriptions

---

# PostgreSQL Requirements

Generate schemas for:

- users
- conversations
- messages
- rooms
- room_members
- tasks
- task_comments
- notifications
- read_receipts

Include:

- indexes
- foreign keys
- timestamps
- soft delete support where useful

---

# Redis Requirements

Use Redis for:

```txt
presence:user:{id}
typing:room:{id}
socket:user:{id}
rate_limit:{userId}
```

Use TTL where appropriate.

---

# UI Requirements

Use Material UI.

Include:

- responsive layout
- sidebar
- chat panel
- task panel
- typing indicators
- online presence indicators
- task status badges
- loading skeletons
- toast notifications

---

# API Requirements

Generate REST endpoints for:

```txt
/auth
/users
/chats
/rooms
/messages
/tasks
/notifications
```

Include request/response examples.

---

# Security Requirements

Implement:

- JWT validation
- rate limiting
- input validation
- secure password hashing
- SQL injection prevention
- XSS-safe rendering

---

# Performance Requirements

Implement:

- pagination
- lazy loading
- optimistic UI
- debounced search
- Redis caching
- efficient WebSocket broadcasting

---

# Testing Requirements

Generate:

## Backend Tests

- unit tests
- integration tests
- websocket event tests

---

# Developer Experience

Include:

- Docker setup
- docker-compose
- environment variable examples
- migration scripts
- seed scripts
- linting
- formatting
- README updates

---

# AI Coding Instructions

When generating code:

- build incrementally
- avoid huge files
- explain important decisions
- prefer readability
- avoid unnecessary abstractions
- use TypeScript types everywhere
- keep functions modular
- preserve the existing project structure
- avoid rewriting working files unless necessary

---

# Expected Deliverables

Generate:

1. Updated architecture plan
2. PostgreSQL schema
3. Redis strategy
4. WebSocket event system
5. Backend implementation
6. Frontend implementation
7. Task module
8. Authentication flow
9. Testing setup
10. Docker setup
11. README updates
12. Development workflow

---

# Important Constraints

- Keep the project beginner/intermediate friendly
- Single backend service only
- No microservices
- No over-engineering
- Prioritize real-time functionality
- Preserve current project structure
- Expand incrementally instead of rebuilding
