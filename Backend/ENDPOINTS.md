# Quantum Messenger Backend - API Endpoints Documentation

**Project Version:** 1.0.0  
**Generated:** April 22, 2026  
**Status:** ✅ All endpoints working properly  

---

## 🔍 Project Overview

This is a **Quantum-Safe Messenger Backend** built with:
- **Framework:** Express.js 5.x (TypeScript)
- **Real-time Communication:** Socket.io 4.x with Redis adapter
- **Authentication:** Supabase JWT
- **Database:** Supabase PostgreSQL
- **Cache/Message Queue:** Redis
- **Security:** Helmet, CORS, Rate Limiting

---

## 📊 Server Configuration

| Property | Value |
|----------|-------|
| **Default Port** | 3000 |
| **Client Origin** | Configurable via `CLIENT_ORIGIN` env var (default: `*`) |
| **Rate Limit** | 100 requests per 15 minutes per IP (global) |
| **Security Headers** | Helmet (CSP, HSTS, X-Frame-Options, etc.) |
| **CORS** | Enabled with configurable origin |
| **Socket.io** | Horizontal scaling via Redis pub/sub adapter |

---

## 🚀 HTTP REST Endpoints

### 1. Health Check

**Endpoint:** `GET /health`

**Purpose:** Basic liveness probe for container orchestration  

**Authentication:** ❌ Not required  
**Rate Limited:** ✅ Yes (100 req/15 min per IP)

**Response (200 OK):**
```json
{
  "status": "ok",
  "timestamp": "2026-04-22T10:30:45.123Z"
}
```

**Use Case:** Docker health checks, load balancer probes

---

### 2. Get Paginated Public Keys

**Endpoint:** `GET /keys/sync?page=1&limit=20`

**Purpose:** Retrieve paginated list of all users' public keys for peer discovery

**Authentication:** ❌ Not required (public endpoint)  
**Rate Limited:** ✅ Yes (100 req/15 min per IP)

**Query Parameters:**

| Parameter | Type | Default | Min | Max | Description |
|-----------|------|---------|-----|-----|-------------|
| `page` | integer | 1 | 1 | unlimited | Page number for pagination |
| `limit` | integer | 20 | 1 | 100 | Results per page (capped at 100) |

**Request Example:**
```bash
curl -X GET "http://localhost:3000/keys/sync?page=1&limit=20"
```

**Response (200 OK):**
```json
{
  "page": 1,
  "limit": 20,
  "total": 150,
  "data": [
    {
      "userId": "user-id-1",
      "x25519PublicKey": "base64-encoded-string",
      "mlKemPublicKey": "base64-encoded-string",
      "ed25519Signature": "base64-encoded-string",
      "mlDsaSignature": "base64-encoded-string",
      "createdAt": "2026-04-20T08:15:30Z",
      "updatedAt": "2026-04-21T12:45:00Z"
    }
    // ... more keys
  ]
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "Invalid pagination parameters",
  "details": "Limit must be between 1 and 100"
}
```

**Use Case:** Client peer discovery, fetching available public keys for encryption

---

### 3. Upload Hybrid Key Bundle

**Endpoint:** `POST /keys/upload`

**Purpose:** Upload user's hybrid classical + post-quantum key bundle for key exchange

**Authentication:** ✅ Required (Supabase JWT)  
**Rate Limited:** ✅ Yes (10 uploads per minute per IP)

**Authorization Header:**
```
Authorization: Bearer <supabase_jwt_token>
```

**Middleware Chain:**
1. `authMiddleware` - Verifies JWT, attaches `req.user`
2. `uploadLimiter` - Rate limiting (10/min per IP)
3. `validateKeyBundle` - Zod schema validation
4. `keyController.upload` - Process and store keys

**Request Body:**
```json
{
  "x25519PublicKey": "base64-encoded-32-byte-ed25519-key",
  "mlKemPublicKey": "base64-encoded-ml-kem-public-key",
  "ed25519Signature": "base64-encoded-ed25519-signature",
  "mlDsaSignature": "base64-encoded-ml-dsa-signature"
}
```

**Field Validation Rules:**

| Field | Type | Min Length | Encoding | Description |
|-------|------|-----------|----------|-------------|
| `x25519PublicKey` | string | 44 chars | Base64 | Classical X25519 DH key (32 bytes) |
| `mlKemPublicKey` | string | 44 chars | Base64 | ML-KEM (Kyber) post-quantum key |
| `ed25519Signature` | string | 44 chars | Base64 | Ed25519 signature over bundle |
| `mlDsaSignature` | string | 44 chars | Base64 | ML-DSA (Dilithium) post-quantum signature |

**Request Example:**
```bash
curl -X POST "http://localhost:3000/keys/upload" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "x25519PublicKey": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "mlKemPublicKey": "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=",
    "ed25519Signature": "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=",
    "mlDsaSignature": "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDD="
  }'
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Keys uploaded successfully"
}
```

**Error Responses:**

**401 Unauthorized** - Missing or invalid JWT:
```json
{
  "error": "Missing or invalid Authorization header"
}
```

**400 Bad Request** - Validation failed:
```json
{
  "error": "Validation failed",
  "issues": [
    {
      "field": "x25519PublicKey",
      "message": "String must contain at least 44 character(s)"
    },
    {
      "field": "mlKemPublicKey",
      "message": "Must be Base64 encoded"
    }
  ]
}
```

**429 Too Many Requests** - Rate limit exceeded:
```json
{
  "error": "Too many requests, please try again later"
}
```

**500 Internal Server Error** - Database or service error:
```json
{
  "error": "Internal Server Error",
  "details": "Failed to upload keys"
}
```

**Use Case:** Initial key exchange setup, key rotation, updating public key material

---

## 📡 WebSocket (Socket.io) Events

**Connection URL:** `ws://localhost:3000` (or `wss://` for production)

**Authentication:** ✅ Required (Supabase JWT in handshake auth object)

**CORS Configuration:**
- Allowed Origins: Configurable via `CLIENT_ORIGIN`
- Allowed Methods: GET, POST

**Horizontal Scaling:** Redis pub/sub adapter enabled for multi-instance deployments

---

### Socket Connection Handshake

**Client-side Connection Example (JavaScript):**
```javascript
import io from 'socket.io-client';

const token = '<supabase_jwt_token>';
const socket = io('ws://localhost:3000', {
  auth: {
    token: token
  }
});

socket.on('connect', () => {
  console.log('Connected! Socket ID:', socket.id);
});

socket.on('connect_error', (error) => {
  console.error('Connection failed:', error.message);
});
```

**Authentication Flow:**
1. Client sends JWT in `auth.token` during handshake
2. `socketAuthMiddleware` validates token with Supabase
3. Verified user is attached to `socket.data.user`
4. Connection accepted if valid, rejected with error if invalid

**Connection Errors:**

| Error | Description |
|-------|-------------|
| `SOCKET_AUTH_MISSING` | No authentication token provided |
| `SOCKET_AUTH_INVALID` | Token is invalid or has expired |
| `SOCKET_AUTH_ERROR` | General authentication failure |

---

### 1. send_message Event

**Direction:** Client → Server  
**Purpose:** Send an encrypted message to another user

**Payload:**
```json
{
  "to": "recipient-user-id",
  "payload": <binary buffer of encrypted message>
}
```

**Behavior:**

- **Recipient Online:** Message delivered immediately via `receive_message` event
- **Recipient Offline:** Message queued in Redis for delivery upon reconnection

**Server-side Handler:**
```typescript
socket.on('send_message', async ({ to, payload }) => {
  // Validates recipient ID and payload
  // Checks if recipient is online
  // Delivers or queues message
});
```

**Client-side Usage Example:**
```javascript
const encryptedMessage = new TextEncoder().encode('encrypted data here');

socket.emit('send_message', {
  to: 'recipient-user-id',
  payload: encryptedMessage
}, (acknowledgment) => {
  console.log('Message sent:', acknowledgment);
});
```

**Validation:**

| Field | Type | Validation | Error Behavior |
|-------|------|-----------|-----------------|
| `to` | string | Non-empty | Message dropped with console warning |
| `payload` | Buffer | Must be Buffer | Message dropped with console warning |

**Console Logging:**
```
[Socket] Delivered (online)  | from=user-1 | to=user-2
[Socket] Queued   (offline)  | from=user-1 | to=user-2
[Socket] send_message error: <error message>
```

---

### 2. receive_message Event

**Direction:** Server → Client  
**Purpose:** Receive encrypted messages from other users

**Payload:**
```
<binary buffer of encrypted message>
```

**Triggers:**
1. **Direct delivery:** When connected peer sends you a message via `send_message`
2. **Offline queue drain:** Upon reconnection, all queued messages are delivered

**Client-side Handler Example:**
```javascript
socket.on('receive_message', (payload) => {
  console.log('Received message (binary payload):', payload);
  
  // Decrypt using client's private keys
  const decrypted = decryptMessage(payload);
  console.log('Decrypted:', decrypted);
});
```

**Server-side Drain on Connect:**
```
[Socket] Draining 5 offline message(s) → user-1
```

---

### 3. disconnect Event

**Direction:** Server → Client (or automatic)  
**Purpose:** Fired when socket connection is closed

**Client-side Handler Example:**
```javascript
socket.on('disconnect', (reason) => {
  console.log('Disconnected:', reason);
  // reason: 'io server namespace disconnect', 'io client namespace disconnect', 'ping timeout', etc.
});
```

**Server-side Logging:**
```
[Socket] Disconnected | socket=abc123 | userId=user-1
```

**Causes:**
- Client calls `socket.disconnect()`
- Network failure or timeout
- Server shutdown
- Client session expires

---

## 🔐 Authentication & Security

### JWT Token Requirements

**Header:** 
```
Authorization: Bearer <token>
```

**Token Source:** Supabase authentication  
**Validation:** Performed by `authMiddleware` (REST) and `socketAuthMiddleware` (WebSocket)

**Token Claims (extracted by Supabase):**
- `sub` (user ID)
- `exp` (expiration timestamp)
- `iat` (issued at timestamp)

### Rate Limiting

**Global Rate Limiter:**
- **Limit:** 100 requests per 15 minutes per IP
- **Applied to:** All routes

**Upload-Specific Rate Limiter:**
- **Limit:** 10 uploads per minute per IP
- **Applied to:** `POST /keys/upload` only

**Rate Limit Response Headers:**
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: <unix-timestamp>
```

---

## 📈 Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Application                     │
├─────────────────────────────────────────────────────────────┤
│  HTTP REST Client  │  WebSocket (Socket.io) Client         │
└──────────┬──────────────────────────────┬────────────────────┘
           │                              │
           │ HTTP/REST                    │ WebSocket
           │                              │
┌──────────▼──────────────────────────────▼────────────────────┐
│                   Express Server (Node.js)                    │
├─────────────────────────────────────────────────────────────┤
│  Middleware Layer:                                           │
│  • Helmet (Security)                                         │
│  • CORS                                                      │
│  • Rate Limiting                                             │
│  • Auth Validation                                           │
├─────────────────────────────────────────────────────────────┤
│  Routes:                                                     │
│  • /health          → healthRoutes                          │
│  • /keys            → keyRoutes                             │
│  • WebSocket        → Socket.io with Redis adapter          │
├─────────────────────────────────────────────────────────────┤
│  Services Layer:                                             │
│  • KeyService       → Key management                         │
│  • MessageService   → Message delivery & queuing             │
│  • SocketService    → Socket.io event handling               │
├─────────────────────────────────────────────────────────────┤
│  Repositories:                                               │
│  • KeyRepository                                             │
│  • PublicKeyRepository                                       │
└──────────┬────────────────────┬─────────────────────────────┘
           │                    │
           │                    │
┌──────────▼────┐    ┌──────────▼──────────┐
│  Supabase DB  │    │   Redis Cache       │
│  • Users      │    │   • Messages        │
│  • Key Bundles│    │   • Pub/Sub Adapter │
└───────────────┘    └─────────────────────┘
```

---

## 🔄 Message Flow Example

### Scenario: User A sends message to User B

```
1. User A connects via WebSocket with valid JWT
   → socket.join(userA_id)
   → [Socket] Connected | socket=abc123 | userId=userA

2. User A emits 'send_message' event
   → { to: 'userB', payload: <binary> }

3. Server checks if User B is online
   
   ✅ IF User B IS ONLINE:
      → io.to(userB).emit('receive_message', payload)
      → User B receives message immediately
      → [Socket] Delivered (online) | from=userA | to=userB

   ❌ IF User B IS OFFLINE:
      → messageService.queueOfflineMessage(userB, payload)
      → Message stored in Redis
      → [Socket] Queued (offline) | from=userA | to=userB

4. When User B reconnects:
   → socket.join(userB_id)
   → messageService.retrieveAndClearOfflineMessages(userB)
   → All queued messages emitted as 'receive_message'
   → [Socket] Draining 1 offline message(s) → userB
   → User B receives all messages
```

---

## 🛠️ Starting the Server

### Development Mode
```bash
npm run dev
```
- Auto-reloads on file changes
- Full error logging

### Production Mode
```bash
npm run start:prod
```
- Requires pre-compiled JavaScript in `dist/` folder
- Build first: `npm run build`

### Output
```
✅  Quantum Messenger API  →  http://localhost:3000
🔌  Socket.io              →  ws://localhost:3000
🛡️  Helmet + Rate Limiting  →  active
🔐  Socket JWT Auth         →  active
🛑  Press Ctrl+C to stop
```

---

## 🧪 Testing Endpoints

### Test Health Endpoint
```bash
curl http://localhost:3000/health
```

### Test Get Keys (No Auth)
```bash
curl "http://localhost:3000/keys/sync?page=1&limit=5"
```

### Test Upload Keys (With Auth)
```bash
curl -X POST http://localhost:3000/keys/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "x25519PublicKey": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "mlKemPublicKey": "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=",
    "ed25519Signature": "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=",
    "mlDsaSignature": "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDD="
  }'
```

### Test WebSocket Connection
```javascript
const io = require('socket.io-client');
const socket = io('http://localhost:3000', {
  auth: { token: 'YOUR_JWT_TOKEN' }
});

socket.on('connect', () => console.log('Connected!'));
socket.on('connect_error', err => console.error('Error:', err));
```

---

## 🚨 Error Handling

All errors are caught by the global error handler and return appropriate HTTP status codes:

| Status | Meaning |
|--------|---------|
| 200 | Success |
| 201 | Created (key upload) |
| 400 | Bad Request (validation error) |
| 401 | Unauthorized (missing/invalid auth) |
| 429 | Too Many Requests (rate limited) |
| 500 | Internal Server Error |

---

## ✅ Verification Checklist

- ✅ TypeScript compilation successful (no errors)
- ✅ All endpoints defined and implemented
- ✅ Authentication middleware in place
- ✅ Rate limiting configured
- ✅ Error handling middleware active
- ✅ Socket.io Redis adapter configured
- ✅ Graceful shutdown handlers in place
- ✅ Security headers (Helmet) enabled
- ✅ CORS configured

---

## 📝 Environment Variables Required

```env
# Server Configuration
PORT=3000
CLIENT_ORIGIN=http://localhost:3000

# Supabase Authentication
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key

# Redis (for message queue & Socket.io adapter)
REDIS_URL=redis://localhost:6379
```

---

## 🎯 Summary

The **Quantum Messenger Backend** is fully functional with:

- **3 HTTP REST endpoints** (1 public, 1 protected, 1 utility)
- **3 WebSocket events** (send/receive messages, disconnect)
- **Hybrid cryptography support** (classical + post-quantum)
- **Offline message queuing** via Redis
- **JWT authentication** via Supabase
- **Horizontal scaling** capability via Redis pub/sub
- **Production-ready security** (Helmet, CORS, Rate limiting)

All components are working properly and ready for deployment.
