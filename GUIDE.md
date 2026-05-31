# WebSocket Chat App Learning Notes (Deno + React + System Design)

This document is a complete consolidated reference of the discussion covering
WebSocket vs Socket.IO, system design, architecture, and next steps for building
a real-time chat application.

---

# 1. WebSocket vs Socket.IO

## 1.1 Native WebSocket

Native WebSocket is a built-in browser and server protocol for real-time
communication.

### Characteristics

- Lightweight
- Fast
- No external dependency
- Works directly with browser API
- Requires manual handling of:
  - reconnection
  - event system
  - connection lifecycle
  - scaling logic

### Example usage

```js
const ws = new WebSocket("ws://localhost:8000");
```

# Real-Time Chat System Notes

---

## 1. WebSocket vs Socket.IO

### WebSocket

**Advantages**

- Full control over behavior
- Better understanding of real-time systems
- Ideal for learning fundamentals
- Works well with Deno

**Disadvantages**

- More manual work
- No built-in features like rooms or auto-reconnect
- Requires custom scaling logic

---

### Socket.IO

Socket.IO is a library built on top of WebSocket.

**Features**

- Automatic reconnection
- Event-based communication
- Rooms and namespaces
- Fallback transport mechanisms
- Simpler API

**Example**

```js
socket.emit("message", data);
socket.on("message", handler);
```

**Advantages**

- Faster development
- Production-ready features
- Easier scaling abstractions

**Disadvantages**

- Extra abstraction layer
- Not pure WebSocket protocol
- Less transparent behavior

---

### Recommendation

- Use native WebSocket first for learning
- Use Socket.IO later for production convenience

---

## 2. Current Backend Understanding

Your backend is already a WebSocket server acting as a gateway.

### Core Logic

```js
const clients = new Set();

for (const client of clients) {
  if (client.readyState === WebSocket.OPEN) {
    client.send(JSON.stringify(message));
  }
}
```

### What this implements

- Client tracking
- Message broadcasting
- Real-time communication

---

## 3. WebSocket readyState

| State      | Value | Meaning                         |
| ---------- | ----- | ------------------------------- |
| CONNECTING | 0     | Connection is being established |
| OPEN       | 1     | Connection is active            |
| CLOSING    | 2     | Connection is closing           |
| CLOSED     | 3     | Connection is closed            |

### Usage

```js
if (client.readyState === WebSocket.OPEN) {
  // safe to send
}
```

---

## 4. WebSocket Gateway

A WebSocket Gateway is a system role, not a technology.

### Definition

A server responsible for managing real-time WebSocket connections.

### Responsibilities

- Maintain active connections
- Receive messages from clients
- Broadcast messages
- Manage lifecycle

---

## 5. Microservices Clarification

### Current system is NOT microservices

```
React frontend
   ↓
WebSocket server (Deno)
   ↓
In-memory Set<WebSocket>
```

This is a **monolithic architecture**.

---

### Microservices concept

- Auth Service
- Chat Service
- Notification Service
- WebSocket Gateway

Each service:

- Runs independently
- Has single responsibility
- Communicates via APIs

---

### Key takeaway

You are building a **modular monolith**, not microservices.

---

## 6. Current Architecture

```
Frontend (React)
   ↓
WebSocket Server (Deno)
   ↓
In-memory client set
   ↓
Broadcast messages
```

### Limitations

- No persistence
- No authentication
- No chat history
- No scaling strategy

---

## 7. Production Architecture (Next Stage)

```
Frontend (React)
   ↓
WebSocket Server
   ↓
Service Layer
   ↓
API Server
   ↓
PostgreSQL Database
```

### Responsibilities

**WebSocket Server**

- Real-time communication
- Broadcasting
- Connection management

**API Server**

- Authentication
- Message history
- Business logic

**Database**

- Users
- Messages
- Rooms

---

## 8. Backend Structure (Improved Design)

```
backend/
  src/
    websocket/
    services/
    db/
    routes/
    app.ts
  main.ts
```

### Layer Responsibilities

**WebSocket Layer**

- Handle connections
- Broadcast messages
- Manage sockets

**Service Layer**

- Validation
- Message formatting
- Business logic

**Database Layer**

- Persistent storage

**API Layer**

- Authentication
- Fetching history

---

## 9. Message Flow

```
React UI
   ↓
WebSocket send
   ↓
Backend receives message
   ↓
Service processes message
   ↓
Database stores message
   ↓
Broadcast to clients
```

---

## 10. Communication Types

### WebSocket (Real-time)

- Messages
- Typing indicators
- Online presence

### REST API (HTTP)

- Login/signup
- Message history
- User data

---

## 11. Roadmap

### Phase 1

- Basic real-time chat
- React ↔ WebSocket ↔ Deno
- Send/receive messages

---

### Phase 2

- Add database
- Store messages
- Load history

---

### Phase 3

- Add REST API
- Authentication
- History endpoints

---

### Phase 4

- Usernames
- Chat rooms
- Auth system

---

### Phase 5

- Redis pub/sub
- Scaling
- Load balancing

---

## 12. Core Mental Model

Instead of:

> Should I use microservices?

Think:

- What belongs to WebSocket layer
- What belongs to API layer
- What belongs to database layer

---

## 13. Key Takeaway

You are building:

- A real-time monolithic chat system

You already built:

- WebSocket server
- Broadcasting system
- Multi-client communication

---

## 14. Final Advice

Don’t over-engineer.

Focus on:

- Working real-time system
- Clean separation of concerns
- Step-by-step scaling
- Strong fundamentals
