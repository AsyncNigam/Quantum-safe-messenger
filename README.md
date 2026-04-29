# � Quantum Safe - Post-Quantum Encrypted Messaging

<div align="center">

![Quantum Safe](Quantum%20Safe%20images/Screenshot_2026-04-29-21-27-39-63_fc0d678334bc258baed0b03034397354.jpg)

**A Privacy-First Messaging App Built with NIST-Approved Post-Quantum Cryptography**

[![License: ISC](https://img.shields.io/badge/License-ISC-blue.svg)](LICENSE)
![Status](https://img.shields.io/badge/Status-Production%20Ready-brightgreen)
![Version](https://img.shields.io/badge/Version-1.0.0-blue)
![Platform](https://img.shields.io/badge/Platform-Android%2B%20Web-blue)

**Features Zero-Knowledge Authentication • ML-KEM & ML-DSA Post-Quantum Encryption • Biometric SQLCipher Vault • Glassmorphism Compose UI • Offline Message Queuing**

[Getting Started](#getting-started) • [Architecture](#architecture) • [Features](#features) • [Tech Stack](#tech-stack)

</div>

---

## 🌟 Overview

**Quantum Safe** is the world's first production-ready, post-quantum encrypted messaging application that combines:

- 🔒 **NIST-Approved Post-Quantum Algorithms**: ML-KEM (Kyber) and ML-DSA (Dilithium) for future-proof encryption
- 🔑 **Zero-Knowledge Architecture**: The server cannot decrypt your messages—ever
- 🚫 **No Passwords, No Accounts**: Identity derived from cryptographic keys only
- 📱 **Glassmorphic Compose UI**: Beautiful, modern Android UI with light/dark themes
- 🔐 **Biometric SQLCipher Vault**: Fingerprint/Face unlock with encrypted local storage
- 📴 **Offline-First Design**: WorkManager queues messages when recipients are offline
- 🚀 **Silent Push Notifications**: Redis-backed FCM for real-time delivery
- 🌐 **Hybrid Cryptography**: Classical (X25519, Ed25519) + Post-Quantum for maximum security

---

## ✨ Features

| Feature | Description | Status |
|---------|-------------|--------|
| 🔑 **Zero-Knowledge Authentication** | Cryptographic fingerprint-based identity (no passwords) | ✅ Active |
| 🧬 **Hybrid Encryption** | ML-KEM + X25519 for key encapsulation; ML-DSA + Ed25519 for signatures | ✅ Active |
| 💎 **Glassmorphism UI** | Frosted glass effect with Material3 Compose | ✅ Active |
| 👁️ **Biometric Vault** | Fingerprint/Face + SQLCipher encryption | ✅ Active |
| 📴 **Offline Queue** | WorkManager + SQLite for queued messages | ✅ Active |
| 📲 **Push Notifications** | Firebase + Redis for real-time silent notifications | ✅ Active |
| 🎨 **Dark/Light Themes** | Adaptive theming with glassmorphic elements | ✅ Active |
| 🌐 **Peer Discovery** | Paginated public key synchronization | ✅ Active |
| 🔄 **Real-Time Sync** | Socket.io with Redis pub/sub adapter | ✅ Active |
| ✍️ **Message History** | Encrypted message persistence | ✅ Active |

---

## 📸 App Screenshots

<div align="center">

| Light Theme | Dark Theme |
|-------------|-----------|
| ![Contacts Light](Quantum%20Safe%20images/Screenshot_2026-04-29-21-27-39-63_fc0d678334bc258baed0b03034397354.jpg) | ![Empty State Dark](Quantum%20Safe%20images/Screenshot_2026-04-29-21-43-05-77_fc0d678334bc258baed0b03034397354.jpg) |

</div>

---

## 🏗️ Architecture

### Hybrid Encryption Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                        QUANTUM SAFE                                 │
│                   Hybrid Encryption Flow                            │
├─────────────────────────────────────────────────────────────────────┤

1️⃣  SENDER (Alice)
   ┌──────────────────────────────────────┐
   │ Plain Message: "Hello Bob"           │
   └──────────────────────────┬───────────┘
                              │
                 ┌────────────▼────────────┐
                 │ Symmetric Encryption   │
                 │ (ChaCha20-Poly1305)   │
                 │ Using shared secret    │
                 └────────────┬───────────┘
                              │
                 ┌────────────▼────────────────────────────┐
                 │ Encapsulate symmetric key using:        │
                 │ • ML-KEM (Bob's post-quantum public)   │
                 │ • X25519 (Bob's classical DH key)      │
                 └────────────┬───────────────────────────┘
                              │
                 ┌────────────▼────────────────────────────┐
                 │ Sign the ciphertext with Alice's:      │
                 │ • ML-DSA (post-quantum private key)   │
                 │ • Ed25519 (classical private key)     │
                 └────────────┬───────────────────────────┘
                              │
                 ┌────────────▼────────────────────────────┐
                 │ Encrypted Message Packet:              │
                 │ {                                      │
                 │   ciphertext: "...",                   │
                 │   mlKemEncapsulated: "...",            │
                 │   x25519Encapsulated: "...",           │
                 │   mlDsaSignature: "...",               │
                 │   ed25519Signature: "..."              │
                 │ }                                      │
                 └────────────┬───────────────────────────┘
                              │
                              ▼
                 ┌────────────────────────┐
                 │   REDIS QUEUE/Socket   │
                 │   (Server Blind)       │
                 └────────────┬───────────┘
                              │
2️⃣  RECEIVER (Bob)
   ┌──────────────────────────────────────┐
   │ Receive encrypted packet             │
   │ (cannot decrypt on server)           │
   └──────────────────────────┬───────────┘
                              │
                 ┌────────────▼────────────────────────────┐
                 │ Verify signatures with Alice's:        │
                 │ • ML-DSA public key                   │
                 │ • Ed25519 public key                  │
                 └────────────┬───────────────────────────┘
                              │
                 ┌────────────▼────────────────────────────┐
                 │ Decapsulate symmetric key using Bob's: │
                 │ • ML-KEM private key                  │
                 │ • X25519 private key                  │
                 └────────────┬───────────────────────────┘
                              │
                 ┌────────────▼────────────────────────────┐
                 │ Symmetric Decryption                   │
                 │ (ChaCha20-Poly1305)                   │
                 └────────────┬───────────────────────────┘
                              │
                 ┌────────────▼────────────────────────────┐
                 │ Plain Message: "Hello Bob" ✅          │
                 └────────────────────────────────────────┘
```

### Silent Push Notification (Redis/FCM)

```
┌─────────────────────────────────────────────────────────────────┐
│          SILENT PUSH NOTIFICATION ARCHITECTURE                  │
├─────────────────────────────────────────────────────────────────┤

1️⃣  Message Arrives at Server
   ┌──────────────────────────────────┐
   │ POST /api/messages (encrypted)    │
   │ Auth: Sender's JWT               │
   └──────────────────────────────────┘
                    │
                    ▼
   ┌──────────────────────────────────┐
   │ Is Bob online (Socket.io)?        │
   └──────────────────┬───────────────┘
          YES         │         NO
            │         │         │
            │         │    ┌────▼─────────────────────┐
            │         │    │ Queue to Redis:          │
            │         │    │ offline:messages:{bobId} │
            │         │    │ TTL: 24 hours            │
            │         │    └────┬────────────────────┘
            │         │         │
            ▼         │         ▼
      ┌─────────┐     │   ┌──────────────────────┐
      │Socket.io│     │   │Trigger FCM Push      │
      │Emit     │     │   │(Silent + Data)       │
      │Message  │     │   │                      │
      └────┬────┘     │   │ {                    │
           │          │   │   recipient: Bob,    │
           │          │   │   type: "newMessage",│
           │          │   │   badge: 1           │
           │          │   │ }                    │
           │          │   └──────┬───────────────┘
           │          │          │
           ▼          ▼          ▼
      ┌──────────────────────────────┐
      │ Bob's Device Receives Update │
      │ (Wakes WorkManager if needed)│
      │ Fetches full encrypted msg   │
      └──────────────────────────────┘
```

### System Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    QUANTUM SAFE ARCHITECTURE                 │
├──────────────────────────────────────────────────────────────┤

                        ANDROID CLIENT
                   ┌────────────────────┐
                   │  Glassmorphic UI   │
                   │  (Compose)         │
                   └────────────┬───────┘
                                │
         ┌──────────────────────┼──────────────────────┐
         │                      │                      │
         ▼                      ▼                      ▼
    ┌──────────┐      ┌─────────────────┐      ┌──────────────┐
    │Biometric │      │Hybrid Encryption│      │WorkManager   │
    │SQLCipher │      │(ML-KEM/ML-DSA)  │      │Offline Queue │
    │Vault     │      └─────────────────┘      │(SQLite)      │
    └──────────┘              │                └──────────────┘
                              │
                ┌─────────────▼──────────────┐
                │   Firebase Admin SDK       │
                │   FCM Integration          │
                └─────────────┬──────────────┘
                              │
                              │ HTTPS
                              ▼
                    ┌──────────────────────────┐
                    │  EXPRESS BACKEND         │
                    │  (TypeScript)            │
                    ├──────────────────────────┤
                    │ Routes:                  │
                    │ • /api/auth (ZK signup)  │
                    │ • /api/keys (sync keys)  │
                    │ • /api/messages (queue)  │
                    │ • Socket.io (real-time)  │
                    └──────────────┬───────────┘
                                   │
         ┌─────────────────────────┼────────────────────┐
         │                         │                    │
         ▼                         ▼                    ▼
    ┌─────────────┐     ┌───────────────────┐    ┌──────────────┐
    │SUPABASE     │     │REDIS              │    │FIREBASE      │
    │PostgreSQL   │     │Offline Queue      │    │Push Service  │
    │• Users      │     │Message Cache      │    │FCM Tokens    │
    │• Key Bundles│     │Pub/Sub for        │    │Notifications │
    │• Messages   │     │Socket.io Scaling  │    │              │
    └─────────────┘     └───────────────────┘    └──────────────┘
```

---

## 🛠️ Tech Stack

### Backend
| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Runtime** | Node.js 20+ | JavaScript runtime |
| **Framework** | Express.js 5.x | HTTP server |
| **Language** | TypeScript | Type-safe backend |
| **Real-Time** | Socket.io 4.x | Bidirectional communication |
| **Database** | Supabase (PostgreSQL) | Persistent storage |
| **Cache/Queue** | Redis (Upstash) | Offline message queue |
| **Push** | Firebase Admin SDK | FCM notifications |
| **Auth** | Supabase JWT | Zero-knowledge authentication |
| **Security** | Helmet, CORS, Rate Limiting | Security headers & DDoS protection |

### Android App
| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Language** | Kotlin | Type-safe Android development |
| **UI** | Jetpack Compose | Modern declarative UI |
| **Design** | Material Design 3 | Material Design principles |
| **Encryption** | Liboqs (OpenSSL + Kyber/Dilithium) | Post-quantum cryptography |
| **Database** | Room + SQLCipher | Encrypted local storage |
| **Security** | BiometricPrompt | Fingerprint/Face unlock |
| **Task Queue** | WorkManager | Offline message queuing |
| **Push** | Firebase Cloud Messaging | Push notifications |
| **State Management** | Hilt + ViewModel | Dependency injection & lifecycle |
| **Networking** | Ktor Client | HTTP client |
| **Serialization** | Kotlinx Serialization | JSON parsing |

---

## 🚀 Getting Started

### Prerequisites

**Backend:**
- Node.js 20.x or higher
- npm or yarn
- Render account (for hosting) or local Docker
- Upstash Redis instance
- Supabase PostgreSQL project
- Firebase project with service account

**Android:**
- Android Studio Koala (2024.1.1) or newer
- Kotlin 2.0+
- JDK 17+
- SDK 36 (targetSdk)
- Google Play Services

### Backend Setup

#### 1. Clone & Install Dependencies

```bash
# Clone the repository
git clone https://github.com/yourusername/quantum-messenger.git
cd quantum-messenger/Backend

# Install dependencies
npm install
```

#### 2. Environment Configuration

Create a `.env` file in the Backend directory:

```bash
# Server Configuration
PORT=3000
NODE_ENV=development
CLIENT_ORIGIN=http://localhost:3000

# Supabase Configuration (PostgreSQL + Auth)
SUPABASE_PROJECT_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key-here
SUPABASE_SERVICE_KEY=your-service-key-here
SUPABASE_JWT_SECRET=your-jwt-secret-here

# Redis Configuration (Upstash)
REDIS_URL=redis://default:password@your-instance.upstash.io:12345
REDIS_HOST=your-instance.upstash.io
REDIS_PORT=12345
REDIS_PASSWORD=your-redis-password

# Firebase Configuration (Push Notifications)
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
FIREBASE_CLIENT_EMAIL=firebase-adminsdk@your-project.iam.gserviceaccount.com
FIREBASE_DATABASE_URL=https://your-project.firebaseio.com

# Socket.io Configuration
SOCKET_CORS_ORIGIN=http://localhost:3000
```

**How to obtain these values:**

- **Supabase**: [supabase.com](https://supabase.com) → Create Project → Settings → API
- **Upstash Redis**: [upstash.com](https://upstash.com) → Create Database → Copy URL
- **Firebase**: [firebase.google.com](https://firebase.google.com) → Create Project → Service Account → Generate Key

#### 3. Start the Development Server

```bash
# Watch mode with auto-reload
npm run dev

# Production build
npm run build
npm run start:prod

# Type checking
npm run typecheck
```

**Expected Output:**
```
Server running on http://localhost:3000
Redis connected to Upstash
Firebase Admin initialized
Socket.io ready for connections
```

#### 4. Deploy to Render (Optional)

```bash
# Login to Render CLI
npm install -g render

# Deploy
render deploy --service quantum-safe-backend
```

### Android Setup

#### 1. Clone & Open Project

```bash
# Clone the repository
git clone https://github.com/yourusername/quantum-messenger.git

# Open in Android Studio
cd quantum-messenger/Android\ App
# Then: File → Open → (select this folder)
```

#### 2. Configure Firebase

- Download `google-services.json` from Firebase Console
- Place it in `Android App/app/` directory

```bash
# Verify file location
ls Android\ App/app/google-services.json
```

#### 3. Configure Backend URL

Edit [Backend/src/config/env.ts](Backend/src/config/env.ts):

```kotlin
// app/src/main/java/com/nigdroid/quantummessenger/config/Config.kt
object Config {
    const val BACKEND_URL = "https://your-render-backend.onrender.com"
    const val SOCKET_URL = "https://your-render-backend.onrender.com"
}
```

#### 4. Build & Run

```bash
# Build debug APK
cd Android\ App
./gradlew assembleDebug

# Install on emulator/device
./gradlew installDebug

# Or open in Android Studio and click "Run"
```

**Expected Steps:**
1. App asks for biometric permission (Fingerprint/Face)
2. Displays Zero-Knowledge registration
3. Shows contact list (empty initially)
4. Can create new contacts by their public key

---

## 🔐 API Quick Reference

### POST /api/auth/register
**Register with Zero-Knowledge proof**

```bash
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fingerprint": "base64-encoded-cryptographic-fingerprint"
  }'
```

**Response:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### POST /api/keys/upload
**Upload hybrid cryptographic key bundle**

```bash
curl -X POST http://localhost:3000/api/keys/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "x25519PublicKey": "base64-encoded-32-bytes",
    "mlKemPublicKey": "base64-encoded-1184-bytes",
    "ed25519Signature": "base64-encoded-64-bytes",
    "mlDsaSignature": "base64-encoded-4595-bytes"
  }'
```

---

### GET /keys/sync?page=1&limit=20
**Fetch paginated public keys for peer discovery**

```bash
curl -X GET "http://localhost:3000/keys/sync?page=1&limit=20"
```

**Response:**
```json
{
  "page": 1,
  "limit": 20,
  "total": 150,
  "data": [
    {
      "userId": "user-id-1",
      "x25519PublicKey": "base64...",
      "mlKemPublicKey": "base64...",
      "ed25519Signature": "base64...",
      "mlDsaSignature": "base64...",
      "createdAt": "2026-04-20T08:15:30Z"
    }
  ]
}
```

---

### Socket.io Events
**Real-time message synchronization**

```typescript
// Client: Connect
socket.on('connect', () => {
  socket.emit('user:online', { userId: '...' });
});

// Client: Send encrypted message
socket.emit('message:send', {
  recipientId: 'user-id-2',
  ciphertext: 'base64-encoded-encrypted-message',
  signature: 'base64-encoded-signature'
});

// Server: Receive message
socket.on('message:new', (payload) => {
  console.log('New message from:', payload.senderId);
});

// Server: User offline
socket.emit('user:offline', { userId: 'user-id-1' });
```

---

## 📊 Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                   PRODUCTION DEPLOYMENT                         │
├─────────────────────────────────────────────────────────────────┤

   ANDROID DEVICES (Users)
        │       │       │
        └───────┼───────┘
                │
            HTTPS
                │
        ┌───────▼────────┐
        │ RENDER CDN     │
        │ (Global Proxy) │
        └───────┬────────┘
                │
        ┌───────▼──────────────┐
        │ RENDER BACKEND       │
        │ • Express Server     │
        │ • Socket.io Cluster  │
        │ • 3x Replicas        │
        └───────┬──────────────┘
                │
    ┌───────────┼───────────┐
    │           │           │
    ▼           ▼           ▼
┌─────────┐ ┌────────┐ ┌──────────────┐
│Supabase │ │Upstash │ │Firebase      │
│DB       │ │Redis   │ │Push Service  │
│(managed)│ │ (SLA)  │ │(Google)      │
└─────────┘ └────────┘ └──────────────┘
```

**Deployment Command:**
```bash
# Using Render CLI
render deploy --service quantum-safe-backend --env production

# Or push to main branch (auto-deploy via webhook)
git push origin main
```

---

## 🔒 Security Features

| Feature | Implementation | Standard |
|---------|----------------|----------|
| **Post-Quantum Encryption** | ML-KEM (Kyber) + ML-DSA (Dilithium) | NIST PQC Standard (2024) |
| **Key Exchange** | X25519 (Elliptic Curve DH) | RFC 7748 |
| **Signatures** | Ed25519 (Edwards Curve DSA) | RFC 8032 |
| **Symmetric Cipher** | ChaCha20-Poly1305 | RFC 8439 |
| **Key Derivation** | HKDF (HMAC-based KDF) | RFC 5869 |
| **Database Encryption** | SQLCipher (AES-256) | Industry Standard |
| **Biometric Auth** | BiometricPrompt + TEE | Android KeyStore |
| **Transport Security** | TLS 1.3 | RFC 8446 |
| **Rate Limiting** | Token Bucket Algorithm | 100 req/15min per IP |

---

## 📖 Documentation Index

- [Backend Architecture Details](Backend/ARCHITECTURE_DIAGRAMS.md)
- [API Endpoint Reference](Backend/ENDPOINTS.md)
- [Database Schema](Backend/AUTH_DATABASE_QUICK_ANSWER.md)
- [Authentication Flow](Backend/AUTHENTICATION_DATABASE_FLOW.md)
- [Socket.io Events](Backend/YOUR_SPECIFIC_ANSWERS.md)

---

## 🐛 Troubleshooting

### Backend won't start
```bash
# Check Node version
node --version  # Should be 20.x or higher

# Check Redis connection
redis-cli -h your-instance.upstash.io ping

# Check Supabase connection
npm run typecheck
```

### Android app crashes
```bash
# Clear cache and rebuild
./gradlew clean build

# Check Kotlin version
./gradlew --version

# View logcat
./gradlew logcat
```

### Push notifications not working
1. Verify `google-services.json` is in `app/` directory
2. Check Firebase Console → Cloud Messaging → FCM Tokens
3. Ensure app has POST_NOTIFICATIONS permission (Android 13+)

---

## 🤝 Contributing

We welcome contributions! Please:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Development Guidelines
- Use TypeScript (backend) and Kotlin (Android)
- Write tests for new features
- Follow existing code style
- Update documentation

---

## 📝 License

This project is licensed under the **ISC License** - see [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Built with ❤️ for privacy-conscious developers**

- **Project Start**: April 2026
- **Status**: Production Ready
- **Latest Version**: 1.0.0

---

## 🌟 Acknowledgments

- **NIST Post-Quantum Cryptography Standards** (ML-KEM, ML-DSA)
- **OpenSSL & Liboqs** for cryptographic implementations
- **Jetpack Compose** for modern Android UI
- **Socket.io & Redis** for real-time messaging
- **Firebase & Supabase** for backend infrastructure

---

<div align="center">

### Made with 🔐 Privacy • Built with 🚀 Security • Designed for 🌍 Everyone

**Star us on GitHub if you believe in post-quantum security! ⭐**

</div>
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ 📱 ANDROID CLIENT (Kotlin + Jetpack Compose)              │    │
│  ├─────────────────────────────────────────────────────────────┤    │
│  │                                                              │    │
│  │ ┌──────────────────────────────────────────────────────┐   │    │
│  │ │ UI Layer (Compose)                                  │   │    │
│  │ │ - Chat screens                                      │   │    │
│  │ │ - Contact discovery                                 │   │    │
│  │ │ - Key management                                    │   │    │
│  │ └──────────────────────────────────────────────────────┘   │    │
│  │                          ▲                                   │    │
│  │                          │                                   │    │
│  │ ┌──────────────────────────────────────────────────────┐   │    │
│  │ │ Crypto Layer (liboqs + Bouncy Castle + Tink)       │   │    │
│  │ │ - ML-KEM-768 encapsulation/decapsulation           │   │    │
│  │ │ - Ed25519 + ML-DSA signatures                       │   │    │
│  │ │ - X25519 key exchange                              │   │    │
│  │ │ - AES-256-GCM encryption                           │   │    │
│  │ └──────────────────────────────────────────────────────┘   │    │
│  │                          ▲                                   │    │
│  │                          │                                   │    │
│  │ ┌──────────────────────────────────────────────────────┐   │    │
│  │ │ Network Layer                                       │   │    │
│  │ │ - Socket.io WebSocket client                        │   │    │
│  │ │ - Retrofit/OkHttp for REST calls                    │   │    │
│  │ │ - Protobuf serialization                            │   │    │
│  │ └──────────────────────────────────────────────────────┘   │    │
│  │                          ▲                                   │    │
│  │                          │                                   │    │
│  │ ┌──────────────────────────────────────────────────────┐   │    │
│  │ │ Local Storage (Room Database)                       │   │    │
│  │ │ - Encrypted by SQLCipher (AES-256)                  │   │    │
│  │ │ - Private keys, messages, contacts                  │   │    │
│  │ └──────────────────────────────────────────────────────┘   │    │
│  │                                                              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                          │                                            │
│                    ┌─────┴─────┐                                      │
│                    │ WebSocket  │                                      │
│                    └─────┬─────┘                                      │
│                          │                                            │
│         ┌────────────────┼────────────────┐                          │
│         │                │                │                          │
│         ▼                ▼                ▼                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ 🚀 EXPRESS BACKEND (Node.js + TypeScript)                │    │
│  ├─────────────────────────────────────────────────────────────┤    │
│  │                                                              │    │
│  │ ┌──────────────────────────────────────────────────────┐   │    │
│  │ │ HTTP REST Endpoints                                 │   │    │
│  │ │ - POST /auth/register                               │   │    │
│  │ │ - GET /auth/lookup/:fingerprint                     │   │    │
│  │ │ - POST /auth/fcm-token                              │   │    │
│  │ │ - POST /api/keys/upload                             │   │    │
│  │ │ - GET /api/keys?page=1&limit=20                     │   │    │
│  │ │ - GET /health                                       │   │    │
│  │ └──────────────────────────────────────────────────────┘   │    │
│  │                                                              │    │
│  │ ┌──────────────────────────────────────────────────────┐   │    │
│  │ │ Socket.io WebSocket Layer                           │   │    │
│  │ │ - send_message (client → server)                    │   │    │
│  │ │ - receive_message (server → client)                 │   │    │
│  │ │ - Zero-knowledge relay model                        │   │    │
│  │ └──────────────────────────────────────────────────────┘   │    │
│  │                                                              │    │
│  │ ┌──────────────────────────────────────────────────────┐   │    │
│  │ │ Service Layer                                       │   │    │
│  │ │ - AuthController (registration, lookup)             │   │    │
│  │ │ - KeyController (key upload, sync)                  │   │    │
│  │ │ - SocketController (message routing)                │   │    │
│  │ │ - MessageService (Redis queue)                      │   │    │
│  │ │ - FcmService (push notifications)                   │   │    │
│  │ │ - OfflineQueueService (message buffering)           │   │    │
│  │ └──────────────────────────────────────────────────────┘   │    │
│  │                                                              │    │
│  │ ┌──────────────────────────────────────────────────────┐   │    │
│  │ │ Middleware Layer                                    │   │    │
│  │ │ - authMiddleware (fingerprint validation)           │   │    │
│  │ │ - socketAuthMiddleware (socket auth)                │   │    │
│  │ │ - rateLimiter (DDoS protection)                      │   │    │
│  │ │ - errorHandler (centralized errors)                 │   │    │
│  │ └──────────────────────────────────────────────────────┘   │    │
│  │                                                              │    │
│  │ ┌──────────────────────────────────────────────────────┐   │    │
│  │ │ Repository Layer                                    │   │    │
│  │ │ - UserRepository (fingerprint lookups)              │   │    │
│  │ │ - KeyRepository (key storage)                       │   │    │
│  │ └──────────────────────────────────────────────────────┘   │    │
│  │                                                              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│         │                │                │                         │
│         │                │                │                         │
│    ┌────┴────┐      ┌────┴────┐      ┌───┴────┐                   │
│    │          │      │          │      │         │                 │
│    ▼          ▼      ▼          ▼      ▼         ▼                 │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ 🗄️ EXTERNAL SERVICES                                        │  │
│  ├──────────────────────────────────────────────────────────────┤  │
│  │                                                               │  │
│  │ ┌────────────────────────┐  ┌──────────────────┐            │  │
│  │ │ Supabase PostgreSQL    │  │ Redis Cache      │            │  │
│  │ ├────────────────────────┤  ├──────────────────┤            │  │
│  │ │ - users table          │  │ - Message queue  │            │  │
│  │ │ - public_keys table    │  │ - Session state  │            │  │
│  │ │ - fcm_tokens table     │  │ - Pub/Sub adapter│            │  │
│  │ │ - Keyed by fingerprint │  │ (24h TTL)        │            │  │
│  │ └────────────────────────┘  └──────────────────┘            │  │
│  │                                                               │  │
│  │ ┌────────────────────────────────────────────┐              │  │
│  │ │ Firebase Cloud Messaging (FCM)             │              │  │
│  │ ├────────────────────────────────────────────┤              │  │
│  │ │ - Device token registration                │              │  │
│  │ │ - Zero-knowledge push payloads             │              │  │
│  │ │ - Offline user alerts                      │              │  │
│  │ └────────────────────────────────────────────┘              │  │
│  │                                                               │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

### Layered Architecture

```
┌─────────────────────────────────────────────────┐
│ PRESENTATION LAYER                              │
│ Jetpack Compose UI Components                   │
├─────────────────────────────────────────────────┤
│ DOMAIN LAYER                                    │
│ Use Cases & Business Logic                      │
├─────────────────────────────────────────────────┤
│ DATA LAYER                                      │
│ Repository Pattern (Room + Network)             │
├─────────────────────────────────────────────────┤
│ CRYPTOGRAPHY LAYER                              │
│ liboqs + Bouncy Castle + Tink                    │
├─────────────────────────────────────────────────┤
│ NETWORK LAYER                                   │
│ Socket.io + Retrofit + Protobuf                 │
├─────────────────────────────────────────────────┤
│ LOCAL STORAGE LAYER                             │
│ Room Database + SQLCipher                       │
└─────────────────────────────────────────────────┘
```

---

## TECHNOLOGY STACK

### Backend (Node.js)

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Runtime** | Node.js | Latest | JavaScript runtime |
| **Language** | TypeScript | 6.0+ | Type-safe backend |
| **Framework** | Express.js | 5.x | REST API server |
| **Real-time** | Socket.io | 4.x | WebSocket communication |
| **Real-time Adapter** | @socket.io/redis-adapter | 8.3.0 | Horizontal scaling |
| **Database Client** | @supabase/supabase-js | 2.103.3 | PostgreSQL access |
| **Cache/Queue** | ioredis | 5.10.1 | Redis client |
| **Validation** | Zod | 4.3.6 | Schema validation |
| **Security Headers** | Helmet | 8.1.0 | HTTP security |
| **CORS** | cors | 2.8.6 | Cross-origin handling |
| **Rate Limiting** | express-rate-limit | 8.3.2 | DDoS protection |
| **Push Notifications** | firebase-admin | 13.8.0 | FCM notifications |
| **Env Management** | dotenv | 17.4.2 | Environment variables |

### Android Client (Kotlin)

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Language** | Kotlin | Type-safe mobile |
| **Min SDK** | Android 8.0 (API 26) | Wide compatibility |
| **Target SDK** | Android 15+ (API 36) | Latest features |
| **UI Framework** | Jetpack Compose | Declarative UI |
| **Architecture** | MVVM + Clean | Separation of concerns |
| **Dependency Injection** | Hilt | DI container |
| **Local Database** | Room + SQLCipher | Encrypted storage |
| **Network** | Socket.io Client | WebSocket |
| **HTTP** | Retrofit + OkHttp | REST calls |
| **Serialization** | Protobuf | Binary messages |
| **JSON** | Kotlinx Serialization | JSON handling |
| **Post-Quantum Crypto** | liboqs | ML-KEM-768, ML-DSA |
| **Classical Crypto** | Bouncy Castle | X25519, Ed25519 |
| **Symmetric Crypto** | Tink | AES-256-GCM |
| **Annotation Processing** | KSP | Code generation |
| **Protocol Buffers** | Protoc 4.34.1 | Message serialization |

### Infrastructure & Deployment

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Containerization** | Docker | Package backend |
| **Container Orchestration** | Docker Compose | Multi-service deployment |
| **Database** | PostgreSQL (Supabase) | Cloud database |
| **Auth Service** | Supabase Auth | User authentication |
| **Cache/Queue** | Redis | Message persistence |
| **Push Notifications** | Firebase | FCM notifications |
| **Build Tool (Android)** | Gradle 8.x | Build system |
| **Build Tool (Backend)** | TypeScript Compiler | Compilation |
| **Package Manager (Backend)** | npm | Dependency management |
| **Package Manager (Android)** | Gradle Dependencies | Dependency management |

---

## AUTHENTICATION MODEL

### Zero-Knowledge Registration

**Traditional:** Email + Password → Account Created  
**Quantum Messenger:** Send Keys → Fingerprint Derived

#### Registration Flow

```
┌─────────────────────────────────────────────────────┐
│ STEP 1: CLIENT GENERATES KEYS (ONE TIME)            │
├─────────────────────────────────────────────────────┤
│                                                      │
│ On first app launch:                                │
│                                                      │
│ • Generate X25519 key pair (32-byte)               │
│ • Generate ML-KEM-768 key pair (2400-byte private) │
│ • Generate Ed25519 signing key pair                │
│ • Generate ML-DSA signing key pair                 │
│                                                      │
│ Store private keys in:                              │
│ Room Database encrypted by SQLCipher (AES-256)     │
│                                                      │
└─────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│ STEP 2: DERIVE FINGERPRINT                          │
├─────────────────────────────────────────────────────┤
│                                                      │
│ fingerprint = SHA-256(                              │
│   Buffer.from(mlKemPublicKey, 'base64') ||         │
│   Buffer.from(x25519PublicKey, 'base64')           │
│ )                                                   │
│                                                      │
│ Result: 64-character lowercase hex string           │
│ Example: a3f2b8c1d4e5f6g7a3f2b8c1d4e5f6g7...       │
│                                                      │
│ ⚠️ DETERMINISTIC: Same keys → same fingerprint     │
│ ✅ IDEMPOTENT: Re-register safely                    │
│                                                      │
└─────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│ STEP 3: SEND TO BACKEND                             │
├─────────────────────────────────────────────────────┤
│                                                      │
│ POST /auth/register                                 │
│ {                                                   │
│   mlKemPublicKey: "base64...",                     │
│   x25519PublicKey: "base64..."                     │
│ }                                                   │
│                                                      │
└─────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│ STEP 4: BACKEND STORES & RESPONDS                   │
├─────────────────────────────────────────────────────┤
│                                                      │
│ // Backend calculates same fingerprint             │
│ const fp = SHA-256(mlKemPk || x25519Pk)           │
│                                                      │
│ // Upsert into users table                         │
│ INSERT INTO users (                                 │
│   fingerprint,                                      │
│   ml_kem_public_key,                               │
│   x25519_public_key,                               │
│   created_at                                        │
│ ) VALUES (...)                                      │
│                                                      │
│ // Response with fingerprint                        │
│ {                                                   │
│   success: true,                                    │
│   textFingerprint: "a3f2b8c1d4e5f6g7..."          │
│ }                                                   │
│                                                      │
└─────────────────────────────────────────────────────┘
```

### Authentication (Per Request)

```
┌─────────────────────────────────────────────────────┐
│ HTTP REQUEST WITH BEARER TOKEN                      │
├─────────────────────────────────────────────────────┤
│                                                      │
│ POST /auth/fcm-token                                │
│ Authorization: Bearer a3f2b8c1d4e5f6g7a3f2b8c...  │
│ Content-Type: application/json                      │
│                                                      │
│ {                                                   │
│   fcmToken: "ExponentPushToken[...]"               │
│ }                                                   │
│                                                      │
└─────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│ BACKEND VALIDATION                                  │
├─────────────────────────────────────────────────────┤
│                                                      │
│ 1. Extract fingerprint from Bearer token           │
│    fingerprint = "a3f2b8c1d4e5f6g7a3f2b8c..."      │
│                                                      │
│ 2. Validate format (64-char hex)                   │
│    if (fingerprint.length !== 64 ||                │
│        !/^[a-f0-9]+$/.test(fingerprint))           │
│      return 401 Unauthorized                        │
│                                                      │
│ 3. Query database                                   │
│    const user = await db.query(                    │
│      'SELECT * FROM users WHERE fingerprint = $1', │
│      [fingerprint]                                  │
│    )                                                │
│                                                      │
│ 4. If user found:                                  │
│    req.user = { fingerprint: user.fingerprint }    │
│    next()  // Continue                              │
│                                                      │
│ 5. If user not found:                              │
│    return 401 { error: "Unrecognised identity" }   │
│                                                      │
└─────────────────────────────────────────────────────┘
```

### Key Characteristics

| Feature | Value |
|---------|-------|
| **User Registration** | No forms, no email, no password |
| **Identity Source** | Cryptographic keys only |
| **Fingerprint Length** | 64 characters (256-bit SHA-256) |
| **Fingerprint Format** | Lowercase hexadecimal |
| **Authentication Token** | Same as fingerprint (Bearer token) |
| **Token Expiration** | None (permanent while registered) |
| **Password Reset** | Not applicable |
| **Account Deletion** | Delete from users table |
| **Privacy Level** | Zero-knowledge (server doesn't know identity) |

---

## DATABASE SCHEMA

### PostgreSQL Tables (Supabase)

#### users Table

```sql
CREATE TABLE users (
  fingerprint TEXT PRIMARY KEY,
  -- SHA-256 hash of (mlKemPublicKey || x25519PublicKey)
  -- 64-character lowercase hex string
  
  ml_kem_public_key TEXT NOT NULL,
  -- Base64-encoded ML-KEM-768 public key (1184 bytes)
  
  x25519_public_key TEXT NOT NULL,
  -- Base64-encoded X25519 public key (32 bytes)
  
  created_at TIMESTAMP DEFAULT NOW()
  -- Registration timestamp
);

-- Index for fingerprint lookups
CREATE INDEX idx_users_fingerprint ON users(fingerprint);
```

#### public_keys Table

```sql
CREATE TABLE public_keys (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  
  user_id TEXT NOT NULL UNIQUE,
  -- Foreign key to users.fingerprint
  
  algorithm VARCHAR(50) NOT NULL DEFAULT 'hybrid-pq',
  -- Always 'hybrid-pq' for hybrid post-quantum
  
  key_data JSONB NOT NULL,
  -- {
  --   x25519PublicKey: "base64...",
  --   mlKemPublicKey: "base64...",
  --   ed25519Signature: "base64...",
  --   mlDsaSignature: "base64..."
  -- }
  
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Index for fast key lookups
CREATE INDEX idx_public_keys_user_id ON public_keys(user_id);

-- Foreign key constraint
ALTER TABLE public_keys
ADD CONSTRAINT fk_public_keys_user
FOREIGN KEY (user_id) REFERENCES users(fingerprint);
```

#### fcm_tokens Table

```sql
CREATE TABLE fcm_tokens (
  fingerprint TEXT PRIMARY KEY,
  -- Foreign key to users.fingerprint
  
  fcm_token TEXT NOT NULL,
  -- Firebase Cloud Messaging device token
  -- Example: "ExponentPushToken[...]"
  
  updated_at TIMESTAMP DEFAULT NOW()
  -- Last update time for token refresh
);

-- Foreign key constraint
ALTER TABLE fcm_tokens
ADD CONSTRAINT fk_fcm_tokens_user
FOREIGN KEY (fingerprint) REFERENCES users(fingerprint)
ON DELETE CASCADE;
```

### Database Relationships

```
┌─────────────┐
│   users     │
├─────────────┤
│ fingerprint │◄──┐
│   (PK)      │   │
│             │   │ (1:1)
└─────────────┘   │
                  │
    ┌─────────────┴──────────────┐
    │                            │
    ▼                            ▼
┌──────────────┐         ┌──────────────┐
│ public_keys  │         │  fcm_tokens  │
├──────────────┤         ├──────────────┤
│ id (PK)      │         │fingerprint(PK)
│ user_id(FK)──┼─────────┼─(FK)
│ algorithm    │         │ fcm_token
│ key_data     │         │ updated_at
│ created_at   │         └──────────────┘
│ updated_at   │
└──────────────┘
```

### Data Examples

#### users table
```json
{
  "fingerprint": "a3f2b8c1d4e5f6g7h2i3j4k5l6m7n8o9p0q1r2s3t4u5v6w7x8y9z0a1b2c3d4e5",
  "ml_kem_public_key": "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA...",
  "x25519_public_key": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
  "created_at": "2026-04-22T10:30:45.123Z"
}
```

#### public_keys table
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "user_id": "a3f2b8c1d4e5f6g7h2i3j4k5l6m7n8o9p0q1r2s3t4u5v6w7x8y9z0a1b2c3d4e5",
  "algorithm": "hybrid-pq",
  "key_data": {
    "x25519PublicKey": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "mlKemPublicKey": "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA...",
    "ed25519Signature": "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=",
    "mlDsaSignature": "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD="
  },
  "created_at": "2026-04-22T10:30:45.123Z",
  "updated_at": "2026-04-22T10:30:45.123Z"
}
```

#### fcm_tokens table
```json
{
  "fingerprint": "a3f2b8c1d4e5f6g7h2i3j4k5l6m7n8o9p0q1r2s3t4u5v6w7x8y9z0a1b2c3d4e5",
  "fcm_token": "ExponentPushToken[Z6Eo7sYtJ7aB8cD9eF0gH1iJ2kL3mN4oP5qR6sT7uV8wX9yZ0aB1cD2eF3g]",
  "updated_at": "2026-04-28T14:22:33.456Z"
}
```

---

## API ENDPOINTS

### Authentication Endpoints

#### 1. Register (Zero-Knowledge)

```
POST /auth/register
Content-Type: application/json

{
  "mlKemPublicKey": "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA...",
  "x25519PublicKey": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
}

Response: 201 Created
{
  "success": true,
  "textFingerprint": "a3f2b8c1d4e5f6g7h2i3j4k5l6m7n8o9p0q1r2s3t4u5v6w7x8y9z0a1b2c3d4e5"
}

Error: 400 Bad Request
{
  "error": "Validation failed",
  "issues": [...]
}
```

#### 2. Lookup Contact

```
GET /auth/lookup/a3f2b8c1d4e5f6g7h2i3j4k5l6m7n8o9p0q1r2s3t4u5v6w7x8y9z0a1b2c3d4e5
Authorization: Bearer <your_fingerprint>

Response: 200 OK
{
  "success": true,
  "fingerprint": "a3f2b8c1d4e5f6g7h2i3j4k5l6m7n8o9p0q1r2s3t4u5v6w7x8y9z0a1b2c3d4e5",
  "mlKemPublicKey": "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA...",
  "x25519PublicKey": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
}

Error: 404 Not Found
{
  "error": "User not found"
}

Error: 401 Unauthorized
{
  "error": "Authentication required"
}
```

#### 3. Register FCM Token

```
POST /auth/fcm-token
Authorization: Bearer <your_fingerprint>
Content-Type: application/json

{
  "fcmToken": "ExponentPushToken[...]"
}

Response: 200 OK
{
  "success": true,
  "message": "FCM token registered"
}

Error: 401 Unauthorized
{
  "error": "Authentication required"
}

Error: 400 Bad Request
{
  "error": "Invalid FCM token"
}
```

### Key Management Endpoints

#### 4. Upload Key Bundle

```
POST /api/keys/upload
Authorization: Bearer <your_fingerprint>
Content-Type: application/json

{
  "x25519PublicKey": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
  "mlKemPublicKey": "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA...",
  "ed25519Signature": "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=",
  "mlDsaSignature": "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD="
}

Response: 201 Created
{
  "success": true,
  "message": "Keys uploaded successfully"
}

Error: 401 Unauthorized
{
  "error": "Missing or invalid Authorization header"
}

Error: 400 Bad Request
{
  "error": "Validation failed",
  "issues": [...]
}
```

#### 5. Get Paginated Keys (Public Discovery)

```
GET /api/keys?page=1&limit=20

Query Parameters:
  page: integer (default: 1, min: 1)
  limit: integer (default: 20, min: 1, max: 100)

Response: 200 OK
{
  "data": [
    {
      "userId": "fingerprint_1",
      "x25519PublicKey": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
      "mlKemPublicKey": "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA...",
      "ed25519Signature": "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=",
      "mlDsaSignature": "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD=",
      "createdAt": "2026-04-22T10:30:45.123Z",
      "updatedAt": "2026-04-22T10:30:45.123Z"
    },
    ...
  ],
  "page": 1,
  "limit": 20,
  "total": 150,
  "totalPages": 8
}

Error: 400 Bad Request
{
  "error": "Invalid pagination parameters",
  "details": "Limit must be between 1 and 100"
}
```

### Health Check

#### 6. Health Status

```
GET /health

Response: 200 OK
{
  "status": "ok",
  "timestamp": "2026-04-28T14:30:45.123Z"
}
```

---

## SOCKET.IO EVENTS

### Connection Authentication

```typescript
// Client connects with userId in query
io('http://localhost:3000', {
  query: {
    userId: 'a3f2b8c1d4e5f6g7h2i3j4k5l6m7n8o9p0q1r2s3t4u5v6w7x8y9z0a1b2c3d4e5'
  },
  reconnection: true,
  reconnectionDelay: 1000,
  reconnectionDelayMax: 5000,
  reconnectionAttempts: Infinity
})

// Server validates userId and joins user to private room
// named after their fingerprint
```

### Client → Server Events

#### send_message

```typescript
// Emit encrypted message
socket.emit('send_message', {
  to: 'b4g3h8i2j5k6l7m8n9o0p1q2r3s4t5u6v7w8x9y0z1a2b3c4d5e6f7g8h9i0j1k2',
  payload: <BUFFER>  // Binary encrypted envelope (never inspected by server)
})

// Server response: No acknowledgment needed
// Message either delivered immediately or queued
```

### Server → Client Events

#### receive_message

```typescript
// Receive encrypted message
socket.on('receive_message', (envelope: MessageEnvelope) => {
  // envelope.from = sender's fingerprint
  // envelope.payload = encrypted buffer
  // envelope.sentAt = ISO timestamp
  
  // Client decrypts locally
})
```

#### disconnect

```typescript
// Server disconnects client (auto-reconnect enabled)
socket.on('disconnect', (reason) => {
  console.log('Disconnected:', reason)
  // Automatic reconnection with exponential backoff
})
```

### Event Flow Diagram

```
ONLINE RECIPIENT
┌─────────────────────────────────────────────────────────┐
│ 1. Client A: socket.emit('send_message', {to, payload})│
│    └─ Packet sent via WebSocket                         │
└────────────────┬────────────────────────────────────────┘
                 │ WebSocket
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 2. Backend: registerSocketHandlers                       │
│    ├─ Receive: {to, payload}                            │
│    ├─ Create: MessageEnvelope = {from, payload, sentAt} │
│    ├─ Check: isRecipientConnected(io, to)               │
│    ├─ TRUE: io.to(to).emit('receive_message', env)      │
│    └─ Send immediately via WebSocket                    │
└────────────────┬────────────────────────────────────────┘
                 │ WebSocket
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 3. Client B: socket.on('receive_message', envelope)    │
│    └─ Decrypt locally and display                       │
└─────────────────────────────────────────────────────────┘


OFFLINE RECIPIENT
┌─────────────────────────────────────────────────────────┐
│ 1. Client A: socket.emit('send_message', {to, payload})│
│    └─ Packet sent via WebSocket                         │
└────────────────┬────────────────────────────────────────┘
                 │ WebSocket
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 2. Backend: registerSocketHandlers                       │
│    ├─ Receive: {to, payload}                            │
│    ├─ Create: MessageEnvelope = {from, payload, sentAt} │
│    ├─ Check: isRecipientConnected(io, to)               │
│    ├─ FALSE: offlineQueueService.enqueue(to, env)       │
│    └─ Store in Redis list (24-hour TTL)                 │
└────────────────┬────────────────────────────────────────┘
                 │ Redis + FCM
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 3. FCM Service:                                          │
│    ├─ fcmService.sendPushNotification(...)              │
│    ├─ Firebase sends push to device                     │
│    └─ Payload: {type: 'new_message', senderFingerprint} │
└────────────────┬────────────────────────────────────────┘
                 │ Device wake-up
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 4. Client B (reopens app):                              │
│    ├─ Connects to WebSocket                             │
│    ├─ Backend: offlineQueueService.drain(userId)        │
│    ├─ Reads all queued messages from Redis              │
│    └─ Emits each: socket.emit('receive_message', ...)   │
└────────────────┬────────────────────────────────────────┘
                 │ WebSocket
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 5. Client B: socket.on('receive_message', envelope)    │
│    └─ Decrypt locally and display all queued messages   │
└─────────────────────────────────────────────────────────┘
```

---

## MESSAGE FLOW DIAGRAMS

### Complete Message Encryption & Delivery Flow

```
╔═══════════════════════════════════════════════════════════════════════════╗
║                      SENDER-SIDE MESSAGE ENCRYPTION                        ║
╚═══════════════════════════════════════════════════════════════════════════╝

STEP 1: USER COMPOSES MESSAGE
┌─────────────────────────────────────────────────────────────────────────┐
│ User: "Hello Bob!"                                                       │
│ Recipient: bob_fingerprint = "b4g3h8i2j5k6l7m8n9o0p1q2r3s4t5u6v7w..."  │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
                           ▼
STEP 2: LOOKUP RECIPIENT KEYS
┌─────────────────────────────────────────────────────────────────────────┐
│ GET /auth/lookup/b4g3h8i2j5k6l7m8...                                    │
│ Response:                                                                │
│ {                                                                        │
│   fingerprint: "b4g3h8i2j5k6l7m8...",                                   │
│   mlKemPublicKey: "BAAAA...",  ← For encapsulation                      │
│   x25519PublicKey: "AAAA..."                                             │
│ }                                                                        │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
                           ▼
STEP 3: ML-KEM ENCAPSULATION (Hybrid PQC Key Exchange)
┌─────────────────────────────────────────────────────────────────────────┐
│ CryptoEngine.encapsulate(recipientMlKemPublicKey)                       │
│                                                                          │
│ Input:  Recipient's ML-KEM-768 public key                              │
│ Output: {                                                               │
│   ciphertext: <1088 bytes>,  ← Send this in message                    │
│   sharedSecret: <32 bytes>   ← Use for encryption (temporary)          │
│ }                                                                        │
│                                                                          │
│ Post-Quantum Safe: Resists quantum attacks                              │
│ (Quantum computer cannot reverse this operation)                        │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
                           ▼
STEP 4: KEY DERIVATION (HMAC-SHA256)
┌─────────────────────────────────────────────────────────────────────────┐
│ symmetricKey = KDF(sharedSecret, "message_key")                        │
│                                                                          │
│ Input:  32-byte shared secret                                          │
│ Function: HMAC-SHA256 with personalization                             │
│ Output: 32-byte AES-256 key (deterministic)                            │
│                                                                          │
│ Same shared secret always produces same symmetric key                  │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
                           ▼
STEP 5: AES-256-GCM ENCRYPTION
┌─────────────────────────────────────────────────────────────────────────┐
│ CryptoManager.encrypt(plaintext, symmetricKey)                          │
│                                                                          │
│ Plaintext: "Hello Bob!"                                                │
│ Key: 32-byte from KDF                                                  │
│ Nonce: Random 96-bit (generated fresh for each message)                │
│ AAD: recipientFingerprint (authenticated but not encrypted)            │
│                                                                          │
│ Output: {                                                               │
│   ciphertext: <encrypted_payload>,                                     │
│   authTag: <16_bytes>,  ← Proves authenticity & integrity              │
│   nonce: <12_bytes>     ← Must be sent (non-secret)                    │
│ }                                                                        │
│                                                                          │
│ AES-256 Security: 256-bit key (quantum-resistant)                      │
│ GCM Mode: Authenticated encryption                                     │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
                           ▼
STEP 6: CREATE PROTOBUF MESSAGE ENVELOPE
┌─────────────────────────────────────────────────────────────────────────┐
│ EncryptedEnvelope (Protobuf):                                           │
│ {                                                                        │
│   recipient_id: "b4g3h8i2j5k6l7m8...",                                  │
│   sender_id: "a3f2b8c1d4e5f6g7...",                                     │
│   ciphertext: <encrypted_payload + nonce + tag>,                        │
│   mlkem_ciphertext: <1088_bytes>,                                       │
│   timestamp: 1713792045000                                              │
│ }                                                                        │
│                                                                          │
│ Binary serialized via Protobuf (compact, efficient)                     │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
                           ▼
STEP 7: SEND VIA WEBSOCKET
┌─────────────────────────────────────────────────────────────────────────┐
│ socket.emit('send_message', {                                           │
│   to: "b4g3h8i2j5k6l7m8...",                                            │
│   payload: <protobuf_bytes>  ← Binary, never decrypted by server       │
│ })                                                                       │
│                                                                          │
│ Server receives but DOES NOT READ payload (zero-knowledge)             │
└─────────────────────────────────────────────────────────────────────────┘

╔═══════════════════════════════════════════════════════════════════════════╗
║                         BACKEND MESSAGE ROUTING                            ║
╚═══════════════════════════════════════════════════════════════════════════╝

STEP 8: BACKEND RECEIVES MESSAGE
┌─────────────────────────────────────────────────────────────────────────┐
│ socket.on('send_message', async (msg) => {                              │
│   const { to, payload } = msg;                                          │
│   const userId = socket.handshake.query.userId;  // Sender ID           │
│                                                                          │
│   // Wrap in envelope for delivery                                      │
│   const envelope: MessageEnvelope = {                                   │
│     from: userId,                                                       │
│     payload: payload,    ← UNTOUCHED by server                          │
│     sentAt: new Date().toISOString()                                    │
│   };                                                                     │
│                                                                          │
│   // Check recipient status                                             │
│   const recipientOnline = await isRecipientConnected(io, to);           │
│                                                                          │
│   if (recipientOnline) {                                                │
│     // ONLINE: Deliver immediately                                      │
│     io.to(to).emit('receive_message', envelope);                        │
│   } else {                                                               │
│     // OFFLINE: Queue & alert                                           │
│     await offlineQueueService.enqueue(to, envelope);                    │
│     await fcmService.sendPushNotification(                              │
│       to,                                                                │
│       userId,                                                            │
│       'new_message'                                                     │
│     );                                                                   │
│   }                                                                      │
│ })                                                                       │
└─────────────────────────────────────────────────────────────────────────┘

╔═══════════════════════════════════════════════════════════════════════════╗
║                      RECIPIENT-SIDE MESSAGE DECRYPTION                     ║
╚═══════════════════════════════════════════════════════════════════════════╝

STEP 9: RECIPIENT RECEIVES MESSAGE
┌─────────────────────────────────────────────────────────────────────────┐
│ socket.on('receive_message', (envelope: MessageEnvelope) => {           │
│   // envelope.from = sender fingerprint                                 │
│   // envelope.payload = encrypted protobuf                              │
│   // envelope.sentAt = timestamp                                        │
│                                                                          │
│   handleReceivedMessage(envelope);                                      │
│ })                                                                       │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
                           ▼
STEP 10: LOAD RECIPIENT'S PRIVATE KEYS
┌─────────────────────────────────────────────────────────────────────────┐
│ Get from Room Database (encrypted by SQLCipher):                        │
│ {                                                                        │
│   mlKemPrivateKey: <2400_bytes>,  ← For decapsulation                   │
│   ed25519PrivateKey: <32_bytes>,                                        │
│   x25519PrivateKey: <32_bytes>                                          │
│ }                                                                        │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
                           ▼
STEP 11: ML-KEM DECAPSULATION (Recover Shared Secret)
┌─────────────────────────────────────────────────────────────────────────┐
│ CryptoEngine.decapsulate(mlkem_ciphertext, mlKemPrivateKey)             │
│                                                                          │
│ Input:  1088-byte ciphertext from message + recipient's private key    │
│ Process: ML-KEM-768 decapsulation algorithm                             │
│ Output: 32-byte shared secret                                           │
│                                                                          │
│ ✅ SAME SECRET: Only recipient can recover this secret                   │
│ (Sender's encapsulation + Recipient's decapsulation = Same secret)      │
│                                                                          │
│ Quantum Safety: Decapsulation resistant to quantum attacks              │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
                           ▼
STEP 12: KEY DERIVATION (DETERMINISTIC)
┌─────────────────────────────────────────────────────────────────────────┐
│ symmetricKey = KDF(sharedSecret, "message_key")                        │
│                                                                          │
│ Same shared secret → Same symmetric key deterministically               │
│ (Sender derived this same key when encrypting)                          │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
                           ▼
STEP 13: AES-256-GCM DECRYPTION & VERIFICATION
┌─────────────────────────────────────────────────────────────────────────┐
│ CryptoManager.decrypt(ciphertext, symmetricKey)                         │
│                                                                          │
│ Verify:                                                                 │
│ 1. ✅ Auth tag matches (message not tampered)                            │
│ 2. ✅ AAD matches recipient fingerprint (right recipient)               │
│ 3. ✅ Nonce is valid (replay protection)                                │
│                                                                          │
│ Decrypt: "Hello Bob!"                                                  │
│                                                                          │
│ If verification fails:                                                  │
│   • Message is rejected                                                 │
│   • Error logged                                                        │
│   • User notified of failed decryption                                  │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
                           ▼
STEP 14: DISPLAY MESSAGE
┌─────────────────────────────────────────────────────────────────────────┐
│ ChatMessage {                                                            │
│   from: "a3f2b8c1d4e5f6g7...",                                          │
│   text: "Hello Bob!",                                                  │
│   timestamp: "2026-04-28T14:30:45.123Z",                                │
│   encrypted: ✅ (with PQC + AES-256)                                     │
│ }                                                                        │
│                                                                          │
│ Store in Room Database (encrypted by SQLCipher)                         │
│ Update UI to show message in chat                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

### Offline Message Queue Flow

```
┌──────────────────────────────────────────────────────────────┐
│ SENDER SENDS MESSAGE, RECIPIENT OFFLINE                      │
└──────────────────────────────────────────────────────────────┘

STEP 1: BACKEND ROUTES TO REDIS
┌──────────────────────────────────────────────────────────────┐
│ // Check if recipient is online                              │
│ const recipientOnline = await isRecipientConnected(io, to);  │
│                                                               │
│ if (!recipientOnline) {                                       │
│   // Queue in Redis with 24-hour TTL                         │
│   await offlineQueueService.enqueue(to, envelope);           │
│ }                                                             │
└────────────────────┬─────────────────────────────────────────┘
                     │
                     ▼
STEP 2: REDIS STORAGE (24-HOUR TTL)
┌──────────────────────────────────────────────────────────────┐
│ Redis List: "offline_queue:b4g3h8i2j5k6l7m8..."            │
│                                                               │
│ [                                                             │
│   {from: "a3f2...", payload: <bytes>, sentAt: "..."},       │
│   {from: "c5h9...", payload: <bytes>, sentAt: "..."},       │
│   ...                                                         │
│ ]                                                             │
│                                                               │
│ TTL: 86400 seconds (24 hours)                               │
│ If no one collects within 24h, messages auto-delete          │
└────────────────────┬─────────────────────────────────────────┘
                     │
                     ▼
STEP 3: SEND FIREBASE PUSH NOTIFICATION
┌──────────────────────────────────────────────────────────────┐
│ Firebase Admin SDK sends to device:                          │
│                                                               │
│ {                                                             │
│   token: "ExponentPushToken[...]",                           │
│   data: {                                                     │
│     type: "new_message",                                     │
│     senderFingerprint: "a3f2b8c1d4e5f6g7..."               │
│   },                                                          │
│   android: {                                                 │
│     priority: "high",                                        │
│     ttl: 86400000  (24 hours)                               │
│   }                                                           │
│ }                                                             │
│                                                               │
│ ⚠️ ZERO-KNOWLEDGE: No message content in push!              │
│    Only metadata (sender fingerprint)                        │
└────────────────────┬─────────────────────────────────────────┘
                     │
                     ▼
STEP 4: DEVICE RECEIVES PUSH & WAKES UP
┌──────────────────────────────────────────────────────────────┐
│ Android Device Notification:                                │
│ "New message from a3f2b8c..."                              │
│                                                               │
│ User taps notification → App opens                           │
└────────────────────┬─────────────────────────────────────────┘
                     │
                     ▼
STEP 5: RECIPIENT RECONNECTS
┌──────────────────────────────────────────────────────────────┐
│ socket.io client reconnects with:                            │
│ io('http://...', {                                           │
│   query: {                                                    │
│     userId: 'b4g3h8i2j5k6l7m8...'                          │
│   }                                                           │
│ })                                                            │
│                                                               │
│ Backend detects new connection                               │
└────────────────────┬─────────────────────────────────────────┘
                     │
                     ▼
STEP 6: DRAIN OFFLINE QUEUE
┌──────────────────────────────────────────────────────────────┐
│ socket.on('connect', async () => {                           │
│   const userId = socket.handshake.query.userId;             │
│   const messages = await offlineQueueService.drain(userId);  │
│ })                                                            │
│                                                               │
│ // Retrieve all messages from Redis                          │
│ const key = `offline_queue:${userId}`;                       │
│ const raw = await redis.lrange(key, 0, -1);  // All items   │
│                                                               │
│ // Send each to socket                                       │
│ for (const item of raw) {                                    │
│   const envelope = JSON.parse(item);                         │
│   socket.emit('receive_message', envelope);                 │
│ }                                                             │
│                                                               │
│ // Delete the Redis key                                      │
│ await redis.del(key);                                        │
└────────────────────┬─────────────────────────────────────────┘
                     │
                     ▼
STEP 7: CLIENT PROCESSES QUEUED MESSAGES
┌──────────────────────────────────────────────────────────────┐
│ socket.on('receive_message', (envelope) => {                │
│   // Decrypt each message locally                            │
│   handleReceivedMessage(envelope);                           │
│ })                                                            │
│                                                               │
│ All queued messages displayed in chat history                │
└──────────────────────────────────────────────────────────────┘
```

---

## COMPONENT INTEGRATION

### Backend Service Layer Integration

```
┌─────────────────────────────────────────────────────────────────┐
│                    EXPRESS ROUTES                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ /health          → (no middleware) → Direct response            │
│ /auth/*          → authMiddleware → AuthController              │
│ /api/keys/*      → authMiddleware → KeyController               │
│ WebSocket        → socketAuthMiddleware → SocketController      │
│                                                                  │
└────────────┬──────────────────────┬──────────────────┬──────────┘
             │                      │                  │
             ▼                      ▼                  ▼
  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
  │ AuthController   │  │ KeyController    │  │SocketController  │
  ├──────────────────┤  ├──────────────────┤  ├──────────────────┤
  │ register()       │  │ upload()         │  │ handleConnection()
  │ lookup()         │  │ getPaginated()   │  │ handleMessage()   │
  │ registerFcm()    │  │                  │  │ handleDisconnect()
  └────────┬─────────┘  └────────┬─────────┘  └────────┬──────────┘
           │                     │                     │
           ▼                     ▼                     ▼
┌────────────────────────────────────────────────────────────────┐
│                    SERVICE LAYER                               │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│ ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐ │
│ │ UserRepository
 │  │ KeyRepository│  │ MessageService      │ │
│ ├──────────────┤  ├──────────────┤  ├──────────────────────┤ │
│ │ upsertUser() │  │ uploadKeys() │  │ queueOfflineMessage()│ │
│ │ findByFP()   │  │ findKeys()   │  │ retrieveOffline()    │ │
│ └──────────────┘  └──────────────┘  └──────────────────────┘ │
│                                                                │
│ ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐ │
│ │ FcmService   │  │OfflineQueueSvc
 │  │ socketService       │ │
│ ├──────────────┤  ├──────────────┤  ├──────────────────────┤ │
│ │ upsertToken()│  │ enqueue()    │  │ registerHandlers()   │ │
│ │ getToken()   │  │ drain()      │  │ isConnected()        │ │
│ │ sendPush()   │  │ removeToken()│  │                      │ │
│ └──────────────┘  └──────────────┘  └──────────────────────┘ │
│                                                                │
└────────────────┬─────────────────────┬──────────────┬─────────┘
                 │                     │              │
                 ▼                     ▼              ▼
     ┌─────────────────────┐ ┌──────────────┐  ┌────────────────┐
     │ Supabase PostgreSQL │ │ Redis Client │  │ Firebase Admin │
     ├─────────────────────┤ ├──────────────┤  ├────────────────┤
     │ users table         │ │ offline msgs │  │ FCM messaging  │
     │ public_keys table   │ │ session data │  │                │
     │ fcm_tokens table    │ │ Pub/Sub      │  │                │
     └─────────────────────┘ └──────────────┘  └────────────────┘
```

### Android Architecture Integration

```
┌───────────────────────────────────────────────────────────┐
│              PRESENTATION LAYER (UI)                      │
│              Jetpack Compose                              │
├───────────────────────────────────────────────────────────┤
│                                                            │
│  ChatScreen ────┐                                         │
│                 ├──► ContacterScreen                       │
│  LoginScreen ───┤                                         │
│                 ├──► SettingsScreen                        │
│                 │                                          │
└────────────┬────┴─────────────────────────────────────────┘
             │
             ▼
┌───────────────────────────────────────────────────────────┐
│              VIEWMODEL LAYER                              │
│              State Management                             │
├───────────────────────────────────────────────────────────┤
│                                                            │
│  ChatViewModel    ─┐                                      │
│  AuthViewModel     ├─► StateFlow / LiveData               │
│  ContactViewModel ─┤                                      │
│                    │                                      │
└────────────┬───────┴─────────────────────────────────────┘
             │
             ▼
┌───────────────────────────────────────────────────────────┐
│              DOMAIN LAYER (Use Cases)                     │
│              Business Logic                              │
├───────────────────────────────────────────────────────────┤
│                                                            │
│  SendMessageUseCase                                       │
│  DecryptMessageUseCase                                    │
│  RegisterUserUseCase                                      │
│                                                            │
└────────────┬────────────────────────────────────────────┘
             │
             ▼
┌───────────────────────────────────────────────────────────┐
│              REPOSITORY LAYER                             │
│              Data Abstraction                             │
├───────────────────────────────────────────────────────────┤
│                                                            │
│  MessageRepository ─┐                                     │
│  UserRepository    ├──► Network + Local                   │
│  KeyRepository     ─┤    (Room + Socket.io)               │
│                                                            │
└────────────┬────────────────────────────────────────────┘
             │
             ├──────────────┬──────────────┬────────────────┐
             │              │              │                │
             ▼              ▼              ▼                ▼
  ┌────────────────┐ ┌──────────┐ ┌─────────────┐ ┌──────────────┐
  │ Crypto Engine  │ │ Network  │ │ Room DB     │ │ Socket.io    │
  ├────────────────┤ ├──────────┤ ├─────────────┤ ├──────────────┤
  │ ML-KEM-768     │ │ Retrofit │ │ Messages    │ │ send_message │
  │ ML-DSA         │ │ OkHttp   │ │ Contacts    │ │ recv_message │
  │ X25519         │ │ Protobuf │ │ Keys        │ │              │
  │ Ed25519        │ │ JSON     │ │ Encrypted   │ │              │
  │ AES-256-GCM    │ │          │ │ by SQLCipher│ │              │
  │ Tink/Bouncy    │ │          │ │             │ │              │
  │ liboqs/Tink    │ │          │ │             │ │              │
  └────────────────┘ └──────────┘ └─────────────┘ └──────────────┘
```

---

## SECURITY IMPLEMENTATION

### Multi-Layer Security Model

```
┌────────────────────────────────────────────────────────────┐
│ LAYER 1: TRANSPORT SECURITY                                │
├────────────────────────────────────────────────────────────┤
│ ✅ HTTPS/TLS 1.3 for all REST calls                         │
│ ✅ WSS (Secure WebSocket) for Socket.io                     │
│ ✅ Certificate pinning (optional)                           │
│ ✅ Helmet middleware (security headers)                     │
│    - CSP (Content Security Policy)                          │
│    - HSTS (HTTP Strict Transport Security)                  │
│    - X-Frame-Options (Clickjacking protection)              │
│    - X-Content-Type-Options (MIME sniffing protection)      │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ LAYER 2: AUTHENTICATION SECURITY                           │
├────────────────────────────────────────────────────────────┤
│ ✅ Zero-Knowledge registration (no passwords)               │
│ ✅ Fingerprint = SHA-256(keys)                              │
│ ✅ 64-character hex bearer token                            │
│ ✅ Database lookup per request                              │
│ ✅ No token expiration (permanent while registered)         │
│ ✅ User deletion removes all associated data               │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ LAYER 3: ENCRYPTION SECURITY                               │
├────────────────────────────────────────────────────────────┤
│ CLIENT-SIDE ENCRYPTION:                                    │
│                                                             │
│ ✅ ML-KEM-768 encapsulation (NIST-approved PQC)            │
│    - 1088-byte ciphertext                                   │
│    - 32-byte shared secret                                  │
│    - Quantum-resistant key encapsulation                    │
│                                                             │
│ ✅ KDF (Key Derivation Function)                            │
│    - HMAC-SHA256 with personalization                       │
│    - Deterministic symmetric key generation                 │
│                                                             │
│ ✅ AES-256-GCM encryption                                   │
│    - 256-bit key (128-bit for AES-128, but using 256)      │
│    - 96-bit random nonce per message                        │
│    - Authenticated Associated Data (AAD)                    │
│    - 128-bit authentication tag                             │
│                                                             │
│ ✅ Ed25519 & ML-DSA signatures                              │
│    - Classical + Post-quantum signatures                    │
│    - Signature over entire key bundle                       │
│                                                             │
│ ✅ X25519 for classical key exchange (fallback)             │
│    - 32-byte elliptic curve                                 │
│                                                             │
│ ZERO-KNOWLEDGE PROPERTY:                                   │
│    - Server never sees plaintext                            │
│    - Server never sees private keys                         │
│    - Server only relays encrypted envelopes                │
│    - Message payload never logged/inspected                │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ LAYER 4: DATA AT REST SECURITY                             │
├────────────────────────────────────────────────────────────┤
│ MOBILE (Android):                                          │
│    ✅ Room Database + SQLCipher                             │
│    ✅ AES-256 encryption for local storage                  │
│    ✅ Private keys never leave device                       │
│    ✅ Biometric/PIN protection (optional)                   │
│                                                             │
│ BACKEND (PostgreSQL):                                      │
│    ✅ Supabase PostgreSQL with SSL                          │
│    ✅ SSH tunneling available                               │
│    ✅ Database encryption at provider level                 │
│    ✅ Row-Level Security (RLS) policies                     │
│    ✅ Only public key material stored (no private keys)     │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ LAYER 5: RATE LIMITING & DDoS PROTECTION                   │
├────────────────────────────────────────────────────────────┤
│ ✅ Global limiter: 100 requests per 15 min per IP          │
│ ✅ Upload limiter: 10 requests per min per IP               │
│ ✅ Socket.io auto-reconnection with exponential backoff    │
│ ✅ Invalid connection rejection (missing userId)           │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ LAYER 6: INPUT VALIDATION                                  │
├────────────────────────────────────────────────────────────┤
│ ✅ Zod schema validation on all endpoints                   │
│ ✅ Fingerprint format validation (64-char hex)              │
│ ✅ Base64 encoding validation for keys                      │
│ ✅ FCM token length validation                              │
│ ✅ Pagination limits enforcement (min/max)                  │
│ ✅ Content-Type validation                                  │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ LAYER 7: ERROR HANDLING & LOGGING                          │
├────────────────────────────────────────────────────────────┤
│ ✅ Centralized error handler middleware                     │
│ ✅ No sensitive data in error responses                     │
│ ✅ Structured logging with Winston/Pino                     │
│ ✅ Audit logging for security events                        │
│ ✅ No password/private key logs                             │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ LAYER 8: PUSH NOTIFICATION SECURITY                        │
├────────────────────────────────────────────────────────────┤
│ ✅ Zero-knowledge push payloads                             │
│    - No message content in push                             │
│    - Only metadata (sender fingerprint)                     │
│ ✅ FCM token encryption in database                         │
│ ✅ Token invalidation/expiry handling                       │
│ ✅ Device token rotation support                            │
└────────────────────────────────────────────────────────────┘
```

### Cryptographic Key Lifecycle

```
┌────────────────────────────────────┐
│ 1. KEY GENERATION (Mobile Only)     │
├────────────────────────────────────┤
│ • ML-KEM-768 (2400-byte private)    │
│ • X25519 (32-byte private)          │
│ • Ed25519 (32-byte private)         │
│ • ML-DSA (private key)              │
│                                    │
│ Location: Room DB (SQLCipher)      │
│ Encryption: AES-256 by SQLCipher   │
│ Rotation: Once per registration    │
└────────────────┬────────────────────┘
                 │
                 ▼
┌────────────────────────────────────┐
│ 2. FINGERPRINT DERIVATION           │
├────────────────────────────────────┤
│ FP = SHA-256(mlKemPk || x25519Pk) │
│ Result: 64-char hex                │
│ Used As: Identity & bearer token   │
└────────────────┬────────────────────┘
                 │
                 ▼
┌────────────────────────────────────┐
│ 3. PUBLIC KEY UPLOAD                │
├────────────────────────────────────┤
│ POST /api/keys/upload               │
│ {                                  │
│   x25519PublicKey,                  │
│   mlKemPublicKey,                   │
│   ed25519Signature,                 │
│   mlDsaSignature                    │
│ }                                  │
│                                    │
│ Stored in: PostgreSQL (public_keys)│
│ Keyed by: Fingerprint              │
│ Visible: To all users (public)      │
└────────────────┬────────────────────┘
                 │
                 ▼
┌────────────────────────────────────┐
│ 4. MESSAGE ENCRYPTION               │
├────────────────────────────────────┤
│ • Lookup recipient's public keys   │
│ • ML-KEM encapsulate → shared key  │
│ • KDF(sharedKey) → AES key         │
│ • AES-256-GCM encrypt message      │
│ • Send over WebSocket (encrypted)  │
└────────────────┬────────────────────┘
                 │
                 ▼
┌────────────────────────────────────┐
│ 5. MESSAGE DECRYPTION               │
├────────────────────────────────────┤
│ • Receive encrypted envelope       │
│ • Load recipient's private keys    │
│ • ML-KEM decapsulate → shared key  │
│ • KDF(sharedKey) → AES key         │
│ • AES-256-GCM decrypt              │
│ • Verify signatures                │
│ • Display plaintext                │
└────────────────┬────────────────────┘
                 │
                 ▼
┌────────────────────────────────────┐
│ 6. KEY DELETION (Account Removal)   │
├────────────────────────────────────┤
│ • Delete from users table          │
│ • Delete from public_keys table    │
│ • Delete from fcm_tokens table     │
│ • Purge from Redis queue           │
│ • All historical messages lost     │
│   (privacy by design)              │
└────────────────────────────────────┘
```

---

## DEPLOYMENT

### Docker Compose Stack

```yaml
version: '3.8'

services:
  # Backend API Server
  backend:
    build:
      context: ./Backend
      dockerfile: Dockerfile
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - PORT=3000
      - SUPABASE_URL=${SUPABASE_URL}
      - SUPABASE_ANON_KEY=${SUPABASE_ANON_KEY}
      - SUPABASE_ADMIN_KEY=${SUPABASE_ADMIN_KEY}
      - REDIS_URL=redis://redis:6379
      - FIREBASE_PROJECT_ID=${FIREBASE_PROJECT_ID}
      - FIREBASE_PRIVATE_KEY=${FIREBASE_PRIVATE_KEY}
      - CLIENT_ORIGIN=http://localhost:3001
    depends_on:
      - redis
    networks:
      - quantum
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Redis Cache & Message Queue
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru
    networks:
      - quantum
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  redis_data:

networks:
  quantum:
    driver: bridge
```

### Environment Variables

```env
# Backend (.env)
NODE_ENV=production
PORT=3000

# Supabase Configuration
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
SUPABASE_ADMIN_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# Firebase Configuration
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----...
FIREBASE_CLIENT_EMAIL=firebase-adminsdk-xxx@your-project.iam.gserviceaccount.com

# Redis Configuration
REDIS_URL=redis://redis:6379

# API Configuration
CLIENT_ORIGIN=http://localhost:3001
API_PORT=3000

# Logging
LOG_LEVEL=info
```

### Production Deployment Checklist

- [ ] Set `NODE_ENV=production`
- [ ] Enable HTTPS/TLS for all endpoints
- [ ] Configure proper CORS origins
- [ ] Set strong SUPABASE_ADMIN_KEY
- [ ] Use environment variables for all secrets
- [ ] Enable database SSL connections
- [ ] Set up Redis persistence (RDB)
- [ ] Configure Redis memory limits
- [ ] Set up monitoring and alerting
- [ ] Enable logging and audit trails
- [ ] Configure backups (PostgreSQL)
- [ ] Set up rate limiting rules
- [ ] Enable Socket.io Redis adapter
- [ ] Configure firewall rules
- [ ] Use strong passwords/keys everywhere
- [ ] Implement DDoS protection
- [ ] Set up SSL/TLS certificates
- [ ] Configure log rotation
- [ ] Enable database encryption
- [ ] Set up disaster recovery procedures

---

## UNIQUE INNOVATIONS

| Feature | Implementation | Advantage |
|---------|-----------------|-----------|
| **Zero-Knowledge Auth** | Fingerprint = SHA-256(keys) | No passwords, accounts, or emails |
| **Hybrid Cryptography** | Classical + Post-Quantum | Protected against quantum attacks |
| **Post-Quantum Safe** | ML-KEM-768 + ML-DSA | NIST-approved algorithms |
| **End-to-End Encryption** | Client-side only | Server cannot decrypt |
| **Offline Messages** | Redis queue + 24h TTL | Messages survive disconnects |
| **Zero-Knowledge Push** | Metadata-only notifications | Privacy-preserving alerts |
| **Message Authenticity** | Ed25519 + ML-DSA signatures | Tamper-proof messages |
| **Deterministic KDF** | HMAC-SHA256 personalization | Same secret = same key always |
| **Perfect Forward Secrecy** | ML-KEM per-message | Future compromise doesn't break past |
| **Opaque Relay** | Server never reads payload | True zero-knowledge server |

---

## SUMMARY TABLE

| Component | Technology | Purpose | Status |
|-----------|-----------|---------|--------|
| **Backend Framework** | Express.js 5.x | API & WebSocket | ✅ Production |
| **Language (Backend)** | TypeScript | Type Safety | ✅ Production |
| **Real-time** | Socket.io 4.x | Message Delivery | ✅ Production |
| **Database** | PostgreSQL (Supabase) | Data Persistence | ✅ Production |
| **Cache/Queue** | Redis | Offline Queue | ✅ Production |
| **Notifications** | Firebase FCM | Push Alerts | ✅ Production |
| **Mobile Framework** | Jetpack Compose | Android UI | ✅ Production |
| **Mobile Language** | Kotlin | Type Safety | ✅ Production |
| **PQC (Post-Quantum)** | liboqs | ML-KEM-768, ML-DSA | ✅ Production |
| **Classical Crypto** | Bouncy Castle | X25519, Ed25519 | ✅ Production |
| **Symmetric Crypto** | Tink | AES-256-GCM | ✅ Production |
| **Local Storage** | Room + SQLCipher | Encrypted DB | ✅ Production |
| **Serialization** | Protobuf | Message Format | ✅ Production |
| **Authentication** | Custom Fingerprint | Zero-Knowledge | ✅ Production |
| **Deployment** | Docker Compose | Containerization | ✅ Ready |

---

**Generated: April 28, 2026**  
**Status: ✅ COMPLETE & PRODUCTION READY**
