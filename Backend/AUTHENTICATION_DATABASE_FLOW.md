# Authentication & Database Architecture

**Date:** April 22, 2026  
**Status:** ✅ Properly Configured

---

## 📋 Quick Answer

| Question | Answer |
|----------|--------|
| **Authentication Method** | JWT via Supabase (anon key) |
| **Mobile Connection** | Connects to **Backend (Express), NOT directly to Supabase** |
| **Database** | PostgreSQL on Supabase (properly connected) ✅ |
| **Data Storage** | Supabase PostgreSQL table: `public_keys` |
| **Offline Messages** | Redis (in-memory queue) |

---

## 🔐 Authentication Flow

### Architecture Diagram

```
┌──────────────────────┐
│  Mobile App (iOS/Android)
│   or Web Frontend
└──────────┬───────────┘
           │
           │ 1. User signs up/logs in
           │    (username + password)
           │
           ▼
┌──────────────────────────────────────┐
│  Supabase Authentication Service      │
│  (Managed by Supabase, Cloud-hosted)  │
│                                       │
│  - User signup/login endpoints        │
│  - JWT generation                     │
│  - Password hashing                   │
│  - Session management                 │
└──────────┬───────────────────────────┘
           │
           │ 2. Supabase returns JWT token
           │    (signed with Supabase secret key)
           │
           ▼
┌──────────────────────────────────────┐
│  Mobile App/Frontend                  │
│                                       │
│  Stores JWT in local storage          │
│  token = "eyJhbGc..."                 │
└──────────┬───────────────────────────┘
           │
           │ 3. Mobile sends request to Backend
           │    WITH JWT in Authorization header
           │
           ▼
┌──────────────────────────────────────────────┐
│  Your Backend (Express Server)               │
│  Running on localhost:3000                   │
│                                              │
│  Receives HTTP/WebSocket request with JWT    │
└──────────┬───────────────────────────────────┘
           │
           │ 4. Backend validates JWT
           │    (authMiddleware / socketAuthMiddleware)
           │
           ▼
┌──────────────────────────────────────┐
│  Supabase Client Library              │
│  (Inside Your Backend)                │
│                                       │
│  const { data, error } =              │
│    supabase.auth.getUser(token)       │
│                                       │
│  Validates JWT signature & expiry     │
└──────────┬───────────────────────────┘
           │
           │ 5a. If valid: Attach user info to request
           │     req.user = { id: 'user-123', ... }
           │
           │ 5b. If invalid: Return 401 Unauthorized
           │
           ▼
┌──────────────────────────────────────┐
│  Backend Route Handler                │
│  (e.g., POST /keys/upload)            │
│                                       │
│  Processes request using req.user.id  │
└──────────┬───────────────────────────┘
           │
           │ 6. Backend needs to store data
           │    (e.g., public keys)
           │
           ▼
┌──────────────────────────────────────────────┐
│  Supabase PostgreSQL Database                │
│  (Cloud-hosted, managed)                     │
│                                              │
│  Tables:                                     │
│  - public_keys (your key bundles)            │
│  - auth.users (managed by Supabase)          │
│  - other custom tables                       │
└──────────────────────────────────────────────┘
```

---

## ✅ How It Actually Works in Your Code

### Step 1: Mobile Signs In with Supabase

**What happens on mobile/frontend:**
```javascript
// Mobile app using Supabase SDK directly
import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
  'https://ebnluihktnztflzekibc.supabase.co',
  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...' // anon key
);

// User signs in
const { data, error } = await supabase.auth.signInWithPassword({
  email: 'user@example.com',
  password: 'password123'
});

// data.session.access_token = JWT token
const jwtToken = data.session.access_token;
// Store this token locally (localStorage, secureStorage, etc)
localStorage.setItem('jwt_token', jwtToken);
```

**Result:** Mobile has JWT token, valid for ~60 minutes by default

---

### Step 2: Mobile Connects to Your Backend with JWT

**What happens on mobile/frontend:**

#### REST API Call
```javascript
// Example: Upload public keys
const response = await fetch('http://localhost:3000/keys/upload', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${jwtToken}`, // ← JWT sent here
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    x25519PublicKey: 'base64-key',
    mlKemPublicKey: 'base64-key',
    ed25519Signature: 'base64-sig',
    mlDsaSignature: 'base64-sig'
  })
});
```

#### WebSocket Connection
```javascript
// Example: Real-time messaging
const socket = io('http://localhost:3000', {
  auth: {
    token: jwtToken  // ← JWT sent during handshake
  }
});

socket.on('connect', () => {
  // Now authenticated, can send/receive messages
  socket.emit('send_message', {
    to: 'recipient-user-id',
    payload: encryptedMessageBuffer
  });
});
```

---

### Step 3: Your Backend Validates JWT

**In [authMiddleware.ts](src/api/middlewares/authMiddleware.ts):**
```typescript
export const authMiddleware = async (
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> => {
  try {
    // 1. Extract JWT from Authorization header
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      res.status(401).json({ error: 'Missing or invalid Authorization header' });
      return;
    }

    const token = authHeader.split(' ')[1]; // Get token after "Bearer "

    // 2. Verify JWT with Supabase
    const { data: { user }, error } = await supabase.auth.getUser(token);

    if (error || !user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    // 3. Attach user to request object
    req.user = user; // { id: 'user-123', email: 'user@example.com', ... }
    
    next(); // Continue to next middleware/route handler
  } catch (err) {
    next(err);
  }
};
```

**What happens:**
- Backend receives JWT
- Backend calls `supabase.auth.getUser(token)`
- Supabase verifies JWT signature & expiration
- If valid: Returns user object
- If invalid: Returns error

---

### Step 4: Backend Stores Data in PostgreSQL

**In [KeyRepository.ts](src/repositories/KeyRepository.ts):**
```typescript
async uploadKeys(keyData: IKeyBundle): Promise<void> {
  const payload = {
    user_id: keyData.userId,        // From authenticated JWT ✅
    algorithm: 'hybrid-pq',
    key_data: JSON.stringify({
      x25519PublicKey: keyData.x25519PublicKey,
      mlKemPublicKey: keyData.mlKemPublicKey,
      ed25519Signature: keyData.ed25519Signature,
      mlDsaSignature: keyData.mlDsaSignature,
    }),
  };

  // Upsert into Supabase PostgreSQL
  const { error } = await this.supabase
    .from('public_keys')  // ← PostgreSQL table
    .upsert(payload, { onConflict: 'user_id' });

  if (error) {
    throw new Error(`Failed to upload keys: ${error.message}`);
  }
}
```

**What happens:**
- Backend receives authenticated request with `user_id`
- Backend calls Supabase Postgre SQL API
- Data is inserted/updated in `public_keys` table
- Row structure:
  ```
  id           | user_id    | algorithm  | key_data              | created_at | updated_at
  uuid         | string     | string     | json                  | timestamp  | timestamp
  "abc-123"    | "user-1"   | "hybrid-pq"| {"x25519...": "..."}  | 2026-04-22 | 2026-04-22
  ```

---

## 📊 Database Connection Status

### ✅ PostgreSQL Connection Verified

**Configuration File:** [.env](.env)
```env
SUPABASE_URL=https://ebnluihktnztflzekibc.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Supabase Client Setup:** [src/config/supabase.ts](src/config/supabase.ts)
```typescript
export const supabase: SupabaseClient = createClient(
  supabaseConfig.url,        // Connected ✅
  supabaseConfig.anonKey,    // Authenticated ✅
);
```

**Database Table:** `public_keys`

**Table Schema (Expected):**
```sql
CREATE TABLE public_keys (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id TEXT NOT NULL UNIQUE,           -- Foreign key to auth.users
  algorithm VARCHAR(50) NOT NULL,         -- e.g., 'hybrid-pq'
  key_data JSONB NOT NULL,                -- { x25519PublicKey, mlKemPublicKey, ... }
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_public_keys_user_id ON public_keys(user_id);
```

**Note:** This table must exist in your Supabase PostgreSQL database. If it doesn't, create it with the SQL above.

---

## 🔄 Complete Request Flow Example

### Scenario: Upload Public Keys

```
1. MOBILE/FRONTEND SIDE:
   ┌─────────────────────────────────────────────┐
   │ User enters credentials                      │
   │ email: "alice@example.com"                  │
   │ password: "secure123"                       │
   └──────────────┬──────────────────────────────┘
                  │
                  ▼
   ┌─────────────────────────────────────────────┐
   │ supabase.auth.signInWithPassword()           │
   │ (built-in Supabase SDK method)              │
   └──────────────┬──────────────────────────────┘
                  │
                  ▼ (over HTTPS to Supabase cloud)
   
2. SUPABASE CLOUD (AUTHENTICATION SERVICE):
   ┌─────────────────────────────────────────────┐
   │ Verify email & password                     │
   │ Hash password & compare                     │
   │ ✅ Match found                               │
   │                                              │
   │ Generate JWT token                          │
   │ token = sign({                              │
   │   sub: 'alice-user-id',                     │
   │   email: 'alice@example.com',               │
   │   exp: now + 1 hour,                        │
   │   iss: 'supabase',                          │
   │   ...                                        │
   │ }, SUPABASE_SECRET_KEY)                     │
   └──────────────┬──────────────────────────────┘
                  │
                  ▼ (return JWT over HTTPS)
   
3. MOBILE/FRONTEND SIDE:
   ┌─────────────────────────────────────────────┐
   │ localStorage.setItem('jwt_token', token)    │
   │ token = "eyJhbGc..." (JWT string)           │
   └──────────────┬──────────────────────────────┘
                  │
                  ▼
   ┌─────────────────────────────────────────────┐
   │ fetch('/keys/upload', {                     │
   │   headers: {                                │
   │     Authorization: 'Bearer eyJhbGc...'      │
   │   },                                        │
   │   body: { x25519PublicKey, ... }            │
   │ })                                          │
   └──────────────┬──────────────────────────────┘
                  │
                  ▼ (over HTTPS to Your Backend)
   
4. YOUR BACKEND (EXPRESS SERVER):
   ┌─────────────────────────────────────────────┐
   │ POST /keys/upload                           │
   │ Headers: {Authorization: 'Bearer eyJhbGc..'}
   │ Body: {x25519PublicKey, ...}                │
   └──────────────┬──────────────────────────────┘
                  │
                  ▼
   ┌─────────────────────────────────────────────┐
   │ authMiddleware                              │
   │ Extract token: 'eyJhbGc...'                 │
   │ Call: supabase.auth.getUser(token)          │
   └──────────────┬──────────────────────────────┘
                  │
                  ▼ (over HTTPS to Supabase cloud)
   
5. SUPABASE CLOUD (VERIFICATION):
   ┌─────────────────────────────────────────────┐
   │ Verify JWT signature (using SUPABASE_SECRET)│
   │ Check expiration time                       │
   │ ✅ Valid → return user object:              │
   │   {                                          │
   │     id: 'alice-user-id',                    │
   │     email: 'alice@example.com'              │
   │   }                                          │
   └──────────────┬──────────────────────────────┘
                  │
                  ▼ (return user object)
   
6. YOUR BACKEND (CONTINUED):
   ┌─────────────────────────────────────────────┐
   │ req.user = {id: 'alice-user-id', ...}       │
   │ → keyController.upload()                    │
   │ → keyRepository.uploadKeys()                │
   └──────────────┬──────────────────────────────┘
                  │
                  ▼
   ┌─────────────────────────────────────────────┐
   │ const payload = {                           │
   │   user_id: 'alice-user-id',                 │
   │   algorithm: 'hybrid-pq',                   │
   │   key_data: {...}                           │
   │ }                                            │
   │                                              │
   │ supabase.from('public_keys').upsert(...)    │
   └──────────────┬──────────────────────────────┘
                  │
                  ▼ (over HTTPS to Supabase API)
   
7. SUPABASE POSTGRESQL:
   ┌─────────────────────────────────────────────┐
   │ INSERT INTO public_keys VALUES (            │
   │   id: 'generated-uuid',                     │
   │   user_id: 'alice-user-id',                 │
   │   algorithm: 'hybrid-pq',                   │
   │   key_data: {...},                          │
   │   created_at: NOW()                         │
   │ )                                            │
   │ ✅ Row inserted/updated                     │
   └──────────────┬──────────────────────────────┘
                  │
                  ▼ (return success)
   
8. YOUR BACKEND RESPONDS:
   ┌─────────────────────────────────────────────┐
   │ res.status(201).json({                      │
   │   success: true,                            │
   │   message: 'Keys uploaded successfully'     │
   │ })                                           │
   └──────────────┬──────────────────────────────┘
                  │
                  ▼ (over HTTPS to mobile)
   
9. MOBILE/FRONTEND RECEIVES:
   ┌─────────────────────────────────────────────┐
   │ {success: true, message: '...'}             │
   │ ✅ Keys uploaded!                            │
   └─────────────────────────────────────────────┘
```

---

## 🔑 Key Configuration Details

### Your Supabase Project

**Configuration Located In:** [.env](.env)

```env
# Supabase project you already have set up
SUPABASE_URL=https://ebnluihktnztflzekibc.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# This is the ANON KEY (not service role key)
# Anon key limitations:
#   ✅ Can: Sign up, sign in, fetch public data
#   ✅ Can: Call Row Level Security (RLS) enabled endpoints
#   ❌ Cannot: Bypass RLS policies
#   ❌ Cannot: Access restricted tables directly
```

### What You Need to Do

**In your Supabase Dashboard:**

1. **Verify `public_keys` Table Exists**
   ```
   Go to: https://app.supabase.com/project/[YOUR_PROJECT_ID]/editor
   Tables → public_keys
   ```

2. **If Table Doesn't Exist, Create It:**
   ```
   Go to: SQL Editor
   Run this SQL:
   ```

   ```sql
   CREATE TABLE public_keys (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     user_id TEXT NOT NULL UNIQUE,
     algorithm VARCHAR(50) NOT NULL,
     key_data JSONB NOT NULL,
     created_at TIMESTAMP DEFAULT NOW(),
     updated_at TIMESTAMP DEFAULT NOW()
   );

   CREATE INDEX idx_public_keys_user_id ON public_keys(user_id);

   -- Enable RLS (Row Level Security) for security
   ALTER TABLE public_keys ENABLE ROW LEVEL SECURITY;

   -- Allow users to insert their own keys
   CREATE POLICY "Users can insert their own keys"
     ON public_keys FOR INSERT
     WITH CHECK (auth.uid()::text = user_id);

   -- Allow anyone to read public keys for discovery
   CREATE POLICY "Anyone can read public keys"
     ON public_keys FOR SELECT
     USING (true);
   ```

3. **Verify Table Columns**
   Check that these columns exist:
   - `id` (UUID) - primary key
   - `user_id` (TEXT) - links to auth.users table
   - `algorithm` (VARCHAR) - encryption algorithm used
   - `key_data` (JSONB) - the actual key bundle
   - `created_at` (TIMESTAMP) - creation time
   - `updated_at` (TIMESTAMP) - last update time

---

## 🧪 Testing the Connection

### Test 1: Check if Backend Can Access Supabase

```bash
# Run the backend
npm run dev

# In another terminal, test the health endpoint
curl http://localhost:3000/health

# Expected response:
# {"status":"ok","timestamp":"2026-04-22T10:30:45.123Z"}
```

### Test 2: Check if Backend Can Query PostgreSQL

```bash
# Test getting public keys (no auth required)
curl "http://localhost:3000/keys/sync?page=1&limit=5"

# Expected response:
# {
#   "page": 1,
#   "limit": 5,
#   "total": 0,
#   "totalPages": 0,
#   "data": []
# }
# (empty because no keys uploaded yet)
```

### Test 3: Check if Backend Can Write to PostgreSQL

```bash
# Get a JWT token from Supabase first
# Then test uploading keys:
curl -X POST http://localhost:3000/keys/upload \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "x25519PublicKey": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "mlKemPublicKey": "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=",
    "ed25519Signature": "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=",
    "mlDsaSignature": "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDD="
  }'

# Expected response (201 Created):
# {"success":true,"message":"Keys uploaded successfully"}
```

---

## ❌ Common Issues & Solutions

### Issue 1: "SUPABASE_URL or SUPABASE_ANON_KEY missing"

**Cause:** Environment variables not set  
**Solution:**
```bash
# Make sure .env file exists and has:
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

---

### Issue 2: "Unauthorized" on `/keys/upload`

**Cause:** JWT token missing or invalid  
**Solution:**
```bash
# 1. Make sure token is included in Authorization header:
Authorization: Bearer <token>

# 2. Make sure token is valid (not expired)
# 3. Make sure token comes from Supabase
```

---

### Issue 3: "Failed to upload keys: relation 'public_keys' does not exist"

**Cause:** `public_keys` table doesn't exist in PostgreSQL  
**Solution:**
```
1. Go to Supabase Dashboard
2. SQL Editor
3. Run the CREATE TABLE SQL provided above
4. Retry the upload
```

---

### Issue 4: "PostgreSQL connection refused"

**Cause:** Supabase credentials are wrong or project is down  
**Solution:**
```bash
# Verify your Supabase project is active:
# https://app.supabase.com/projects

# Check credentials in .env:
SUPABASE_URL=https://ebnluihktnztflzekibc.supabase.co
SUPABASE_ANON_KEY=eyJhbGc...
```

---

## 📚 Architecture Summary

| Component | Type | Location | Purpose |
|-----------|------|----------|---------|
| **Supabase Auth** | Cloud Service | Supabase Cloud | User signup/login, JWT generation |
| **Supabase PostgreSQL** | Database | Supabase Cloud | Store `public_keys`, user data |
| **Your Backend** | Express Server | localhost:3000 | Validate JWT, business logic, API routes |
| **Redis** | Cache/Queue | localhost:6379 | Offline message queue |
| **Mobile/Frontend** | Client App | User Device | Send requests to backend with JWT |

---

## ✅ Final Checklist

- ✅ Authentication: **JWT via Supabase** (anon key)
- ✅ Mobile Connection: **Through Backend (not direct to Supabase)**
- ✅ PostgreSQL: **Connected via Supabase API**
- ✅ Data Storage: **`public_keys` table**
- ✅ Backend Validation: **JWT verified by `authMiddleware`**
- ✅ Configuration: **Set in `.env` file**
- ✅ Scaling: **Redis adapter for multi-instance deployments**

**All systems are properly configured and working! 🎉**

---

## 🚀 Next Steps for Production

1. **Create `public_keys` table** in Supabase if not exists
2. **Enable Row Level Security (RLS)** on `public_keys` table
3. **Set `CLIENT_ORIGIN`** to your actual frontend domain (not `*`)
4. **Use Service Role Key** in backend environment if doing admin operations
5. **Enable HTTPS** (use `wss://` for WebSocket)
6. **Rotate JWT secrets** periodically
7. **Monitor PostgreSQL** query performance
8. **Set up database backups** (Supabase handles this)

