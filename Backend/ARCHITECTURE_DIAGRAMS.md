# Authentication & Database Architecture - Visual Diagrams

## Flow Diagram 1: User Authentication Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    MOBILE/WEB APP                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  User enters credentials:                                       │
│  ┌──────────────────────────┐                                  │
│  │ email: alice@example.com │                                  │
│  │ password: secure123      │                                  │
│  └──────────┬───────────────┘                                  │
│             │                                                   │
│             ▼                                                   │
│  const { data } = await supabase.auth.signInWithPassword(...)  │
│  ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼    │
└─────────────────────────────────────────────────────────────────┘
           │ HTTPS REQUEST │
           │ (credentials) │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│               SUPABASE AUTHENTICATION SERVICE                   │
│                      (Cloud Hosted)                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Receive credentials from mobile                            │
│  2. Query auth.users table for email                           │
│  3. Hash password & compare with stored hash                   │
│  4. ✅ Match found!                                             │
│  5. Generate JWT token signed with SUPABASE_SECRET_KEY        │
│                                                                 │
│  JWT Token = sign({                                            │
│    sub: '11111111-1111-1111-1111-111111111111',               │
│    email: 'alice@example.com',                                 │
│    exp: 1719345645,  (1 hour from now)                        │
│    iss: 'supabase',                                            │
│    aud: 'authenticated',                                       │
│    role: 'authenticated'                                       │
│  }, SUPABASE_SECRET_KEY)                                       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
           │ HTTPS RESPONSE │
           │ (JWT token)    │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MOBILE/WEB APP                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Receive JWT from Supabase:                                   │
│                                                                  │
│  jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."        │
│                                                                  │
│  Store securely:                                               │
│  localStorage.setItem('jwt_token', jwtToken)                   │
│                                                                  │
│  ✅ Now ready to authenticate requests!                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Flow Diagram 2: REST API Request with JWT

```
┌─────────────────────────────────────────────────────────────────┐
│                    MOBILE/WEB APP                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  jwtToken = localStorage.getItem('jwt_token')                  │
│                                                                  │
│  fetch('http://localhost:3000/keys/upload', {                 │
│    method: 'POST',                                             │
│    headers: {                                                   │
│      Authorization: `Bearer ${jwtToken}`,                      │
│      Content-Type: 'application/json'                          │
│    },                                                           │
│    body: JSON.stringify({                                      │
│      x25519PublicKey: '...',                                   │
│      mlKemPublicKey: '...',                                    │
│      ed25519Signature: '...',                                  │
│      mlDsaSignature: '...'                                     │
│    })                                                           │
│  })                                                             │
│                                                                  │
│  ┌─────────────────────────────────────────────────┐           │
│  │ REQUEST HEADERS:                                 │           │
│  │ Authorization: Bearer eyJhbGc...                 │           │
│  │ Content-Type: application/json                   │           │
│  │                                                  │           │
│  │ REQUEST BODY:                                   │           │
│  │ {                                               │           │
│  │   x25519PublicKey: '...',                       │           │
│  │   mlKemPublicKey: '...',                        │           │
│  │   ed25519Signature: '...',                      │           │
│  │   mlDsaSignature: '...'                         │           │
│  │ }                                               │           │
│  └─────────────────────────────────────────────────┘           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
           │ HTTPS POST /keys/upload │
           │ (with Authorization header)
           ▼
┌─────────────────────────────────────────────────────────────────┐
│            YOUR BACKEND (EXPRESS SERVER)                        │
│            localhost:3000                                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. REQUEST RECEIVED:                                          │
│     POST /keys/upload                                          │
│     Authorization: Bearer eyJhbGc...                           │
│     Body: {...key data...}                                    │
│                                                                  │
│  2. authMiddleware runs:                                       │
│     - Extract token from Authorization header                  │
│     - token = 'eyJhbGc...'                                    │
│     - Call: await supabase.auth.getUser(token)                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
           │ HTTPS REQUEST │
           │ (validate JWT) │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│               SUPABASE JWT VERIFICATION                         │
│                      (Cloud Hosted)                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Receive JWT token from backend                             │
│  2. Verify JWT signature using SUPABASE_SECRET_KEY             │
│  3. Check if token is expired                                  │
│  4. ✅ Valid!                                                   │
│  5. Return user object:                                        │
│     {                                                           │
│       id: '11111111-1111-1111-1111-111111111111',             │
│       email: 'alice@example.com',                              │
│       role: 'authenticated'                                    │
│     }                                                           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
           │ HTTPS RESPONSE │
           │ (user object)  │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│            YOUR BACKEND (EXPRESS SERVER) - CONTINUED           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  3. authMiddleware stores user:                                │
│     req.user = {                                               │
│       id: '11111111-1111-1111-1111-111111111111',             │
│       email: 'alice@example.com',                              │
│       role: 'authenticated'                                    │
│     }                                                           │
│     next() → Continue to next middleware                       │
│                                                                  │
│  4. validateKeyBundle middleware:                              │
│     - Validate request body against Zod schema                 │
│     - ✅ Valid                                                  │
│     - next()                                                    │
│                                                                  │
│  5. keyController.upload() runs:                               │
│     const userId = req.user.id  ← From JWT!                   │
│     await keyRepository.uploadKeys({                           │
│       userId,                                                  │
│       x25519PublicKey: req.body.x25519PublicKey,             │
│       mlKemPublicKey: req.body.mlKemPublicKey,               │
│       ed25519Signature: req.body.ed25519Signature,           │
│       mlDsaSignature: req.body.mlDsaSignature                │
│     })                                                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
           │ (call to repository layer)
           ▼
┌─────────────────────────────────────────────────────────────────┐
│            YOUR BACKEND - KEY REPOSITORY                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  uploadKeys(bundle) {                                          │
│    const payload = {                                           │
│      user_id: bundle.userId,       ← From JWT                 │
│      algorithm: 'hybrid-pq',                                   │
│      key_data: JSON.stringify({                                │
│        x25519PublicKey: bundle.x25519PublicKey,              │
│        mlKemPublicKey: bundle.mlKemPublicKey,                │
│        ed25519Signature: bundle.ed25519Signature,            │
│        mlDsaSignature: bundle.mlDsaSignature                 │
│      })                                                         │
│    }                                                            │
│                                                                  │
│    await supabase.from('public_keys').upsert(payload, ...)    │
│  }                                                              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
           │ HTTPS API CALL │
           │ (upsert data) │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│        SUPABASE POSTGRESQL DATABASE                             │
│                      (Cloud Hosted)                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  TABLE: public_keys                                            │
│  ┌──────────┬──────────────────────────┬──────────┬──────────┐ │
│  │ id       │ user_id                  │ algorithm│ key_data │ │
│  ├──────────┼──────────────────────────┼──────────┼──────────┤ │
│  │ uuid-123 │ 11111111-1111-1111-1111- │ hybrid-pq│ {...}   │ │
│  │          │ 111111111111            │          │          │ │
│  └──────────┴──────────────────────────┴──────────┴──────────┘ │
│                                                                  │
│  INSERT/UPDATE executed:                                        │
│  INSERT INTO public_keys VALUES (                              │
│    id: gen_random_uuid(),                                      │
│    user_id: '11111111-1111-1111-1111-111111111111',           │
│    algorithm: 'hybrid-pq',                                     │
│    key_data: '{"x25519...": "..."}',                          │
│    created_at: NOW(),                                          │
│    updated_at: NOW()                                           │
│  )                                                              │
│                                                                  │
│  ✅ Row inserted/updated successfully                           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
           │ HTTPS RESPONSE │
           │ (success)      │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│            YOUR BACKEND - RESPONSE                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  res.status(201).json({                                        │
│    success: true,                                              │
│    message: 'Keys uploaded successfully'                       │
│  })                                                             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
           │ HTTPS RESPONSE │
           │ (201 Created)  │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MOBILE/WEB APP                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Response received:                                            │
│  {                                                             │
│    success: true,                                             │
│    message: 'Keys uploaded successfully'                      │
│  }                                                             │
│                                                                  │
│  ✅ Keys successfully uploaded to PostgreSQL!                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Flow Diagram 3: WebSocket Connection with JWT

```
┌─────────────────────────────────────────────────────────────────┐
│                    MOBILE/WEB APP                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  import io from 'socket.io-client'                             │
│                                                                  │
│  const socket = io('http://localhost:3000', {                 │
│    auth: {                                                      │
│      token: localStorage.getItem('jwt_token')                  │
│    }                                                            │
│  })                                                             │
│                                                                  │
│  ┌─────────────────────────────────────┐                       │
│  │ HANDSHAKE:                           │                       │
│  │ - Socket.io client connects          │                       │
│  │ - Sends JWT in auth.token            │                       │
│  │ - Waits for authentication result    │                       │
│  └─────────────────────────────────────┘                       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
           │ WebSocket CONNECT │
           │ (with JWT)        │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│            YOUR BACKEND (EXPRESS SERVER)                        │
│            Socket.io with Redis Adapter                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  io.use(socketAuthMiddleware)  ← Runs on every connection      │
│                                                                  │
│  socketAuthMiddleware receives:                                │
│  socket.handshake.auth.token = 'eyJhbGc...'                   │
│                                                                  │
│  Calls: await supabase.auth.getUser(token)                    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
           │ HTTPS REQUEST │
           │ (validate JWT) │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│               SUPABASE JWT VERIFICATION                         │
│                      (Cloud Hosted)                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Receive JWT from backend                                   │
│  2. Verify JWT signature                                       │
│  3. ✅ Valid!                                                   │
│  4. Return user object                                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
           │ HTTPS RESPONSE │
           │ (user object)  │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│            YOUR BACKEND - SOCKET.IO HANDSHAKE                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  socketAuthMiddleware completes:                               │
│  socket.data.user = {                                          │
│    id: '11111111-1111-1111-1111-111111111111',                │
│    email: 'alice@example.com'                                  │
│  }                                                              │
│                                                                  │
│  Socket authenticated! ✅                                       │
│                                                                  │
│  socket.join(userId)  ← Join room with their user ID          │
│  handleConnection() runs:                                      │
│    - [Socket] Connected | socket=abc123 | userId=alice       │
│    - messageService.retrieveAndClearOfflineMessages()         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
           │ WebSocket CONNECT ACK │
           │ (connection accepted)  │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MOBILE/WEB APP                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  socket.on('connect', () => {                                 │
│    console.log('Connected!');                                  │
│                                                                  │
│    // Now can send messages                                    │
│    socket.emit('send_message', {                              │
│      to: 'recipient-id',                                       │
│      payload: encryptedBuffer                                  │
│    });                                                          │
│  })                                                             │
│                                                                  │
│  socket.on('receive_message', (payload) => {                  │
│    // Receive messages from other users                        │
│    console.log('Message received!', payload);                 │
│  })                                                             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Architecture Overview: Complete System

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                            YOUR INFRASTRUCTURE                                 │
├───────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌──────────────────────┐         ┌──────────────────────┐                    │
│  │  SUPABASE CLOUD      │         │  YOUR BACKEND        │                    │
│  │  (Remote Server)     │         │  (Express + Node)    │                    │
│  │                      │         │  localhost:3000      │                    │
│  ├──────────────────────┤         ├──────────────────────┤                    │
│  │ • Auth Service       │◄────────│ ✅ Connected        │                    │
│  │   - JWT generation   │ HTTPS   │                      │                    │
│  │   - User mgmt        │         │ Routes:              │                    │
│  │   - Session mgmt     │         │ • GET /health        │                    │
│  │                      │         │ • GET /keys/sync     │                    │
│  │ • PostgreSQL DB      │         │ • POST /keys/upload  │                    │
│  │   - auth.users       │         │ • WebSocket (msgs)   │                    │
│  │   - public_keys      │         │                      │                    │
│  │   - (other tables)   │         │ Middleware:          │                    │
│  │                      │         │ • authMiddleware     │                    │
│  │                      │         │ • socketAuthMW       │                    │
│  │                      │         │ • rateLimiter        │                    │
│  └──────────────────────┘         └──────────────────────┘                    │
│           ▲                                  ▲                                  │
│           │                                  │                                 │
│           └──────────────────────────────────┘                                 │
│                                                                                 │
│  ┌──────────────────────┐         ┌──────────────────────┐                    │
│  │  REDIS (localhost)   │◄────────│ Backend              │                    │
│  │  localhost:6379      │         │ (Message Queue)      │                    │
│  │                      │         │                      │                    │
│  │ • Offline messages   │         │ Socket.io uses:      │                    │
│  │   stored here        │         │ • Redis adapter      │                    │
│  │ • Pub/Sub for        │         │ • Horizontal scaling │                    │
│  │   Socket.io scaling  │         │                      │                    │
│  └──────────────────────┘         └──────────────────────┘                    │
│                                                                                 │
└───────────────────────────────────────────────────────────────────────────────┘
           ▲                                          ▲
           │ HTTPS                                    │ HTTP/WebSocket
           │ (for validation)                         │
           │                                          │
┌──────────┴──────────────────────────────────────────┴──────────┐              │
│                  MOBILE/WEB CLIENT                              │              │
├──────────────────────────────────────────────────────────────────┤             │
│                                                                   │             │
│  1. Sign in directly with Supabase ─────► Get JWT token         │             │
│  2. Send requests to Backend ──────────► With JWT in header      │             │
│  3. Connect to Backend WebSocket ──────► With JWT in handshake   │             │
│  4. Encrypted messages through Backend ◄─ Via WebSocket          │             │
│                                                                   │             │
└──────────────────────────────────────────────────────────────────┘             │
```

---

## Data Flow: Message Storage & Retrieval

```
SCENARIO: Alice sends a message to Bob

─────────────────────────────────────────────────────────────────

STEP 1: Bob is ONLINE

Alice                           Your Backend                Bob
  │                                  │                        │
  ├─ send_message ─────────────────► │                        │
  │  (payload)                        │                        │
  │                                  ├─ Check if Bob online ──► ✅ Yes
  │                                  │                         │
  │                                  │  emit('receive_message')│
  │                                  │  ◄─────────────────────┤
  │                                  │  (payload delivered)    │
  │  ✅ Delivered immediately         │                        │
  │                                  │                         │

─────────────────────────────────────────────────────────────────

STEP 2: Bob is OFFLINE

Alice                  Your Backend              Redis Queue
  │                         │                        │
  ├─ send_message ────────► │                        │
  │  (payload)              │                        │
  │                        ├─ Check if Bob online ──► ❌ No
  │                        │                         │
  │                        ├─ Queue message in Redis │
  │                        │  messageService        │
  │                        │  .queueOfflineMessage()│
  │                        │ ─────────────────────► │
  │                        │                        ├─ Store in queue
  │  ✅ Queued              │                        │
  │  (waiting for Bob)      │                        │
  │                        │                        │
  
  [Time passes...]
  
  Bob reconnects:
  
  Your Backend            Redis Queue             Bob
      │                        │                   │
      ├─ Bob connects ◄────────┤                   ├─ connect
      │  (Socket authenticated)│                   │
      │                        │                   │
      ├─ retrieveAndClear()────►                   │
      │  offline messages      ├─ Return messages ─┤
      │                        │                   │
      │  messageService       ◄──────────────────┤ ├─ [stored]
      │  Drain 1 offline msg  │  emit('receive'  │ │
      │  emit to Bob          │  message')        │ │
      │ ─────────────────────► ├─ Delete from queue
      │                        │
      │  ✅ Message delivered  │
      │  (from queue)          │

─────────────────────────────────────────────────────────────────
```

---

## PostgreSQL Table Structure

```
TABLE: public_keys

Row created when user uploads keys:

┌─────────────────────────────────────────────────────────────────┐
│ Column    │ Type      │ Value Example                           │
├─────────────────────────────────────────────────────────────────┤
│ id        │ UUID      │ "a1b2c3d4-e5f6-7a8b-9c0d-e1f2g3h4i5j6" │
│ user_id   │ TEXT      │ "11111111-1111-1111-1111-111111111111"  │
│ algorithm │ VARCHAR   │ "hybrid-pq"                             │
│ key_data  │ JSONB     │ {                                       │
│           │           │   "x25519PublicKey": "base64...",       │
│           │           │   "mlKemPublicKey": "base64...",        │
│           │           │   "ed25519Signature": "base64...",      │
│           │           │   "mlDsaSignature": "base64..."         │
│           │           │ }                                       │
│ created_at│ TIMESTAMP │ "2026-04-22 10:30:45.123456+00"         │
│ updated_at│ TIMESTAMP │ "2026-04-22 10:30:45.123456+00"         │
└─────────────────────────────────────────────────────────────────┘

Query Examples:

-- Get all public keys (for key discovery)
SELECT id, user_id, algorithm, key_data, created_at 
FROM public_keys 
ORDER BY created_at DESC 
LIMIT 20;

-- Get specific user's keys
SELECT * FROM public_keys WHERE user_id = '11111111-1111-1111-1111-111111111111';

-- Upsert (insert or update)
INSERT INTO public_keys (user_id, algorithm, key_data)
VALUES ('11111111-1111-1111-1111-111111111111', 'hybrid-pq', '{"..."}')
ON CONFLICT (user_id) DO UPDATE SET key_data = EXCLUDED.key_data;
```

---

## Summary Table: What Connects Where

```
┌────────────────────┬─────────────────────┬────────────────────┬──────┐
│ Component          │ Connected Via       │ Purpose            │ Status
├────────────────────┼─────────────────────┼────────────────────┼──────┤
│ Mobile App         │ HTTPS + WebSocket   │ Frontend           │ ✅   │
│ Backend            │ HTTP                │ API Server         │ ✅   │
│ Supabase Auth      │ HTTPS               │ JWT Generation     │ ✅   │
│ PostgreSQL         │ Supabase REST API   │ Data Storage       │ ✅   │
│ Redis              │ Direct TCP          │ Message Queue      │ ✅   │
│ Socket.io          │ WebSocket + Redis   │ Real-time Messaging│ ✅   │
└────────────────────┴─────────────────────┴────────────────────┴──────┘
```

---

## Key Points Summary

✅ **JWT comes from Supabase** - Not generated by your backend  
✅ **Mobile connects to Backend** - Not directly to Supabase (except login)  
✅ **Backend validates JWT** - By calling Supabase  
✅ **Data stored in PostgreSQL** - Via Supabase API  
✅ **Messages queued in Redis** - For offline delivery  
✅ **Everything properly connected** - All systems working!

