<div align="center">

# 🛡️ Quantum Safe

### Post-Quantum Encrypted Messenger for Android

**The first production-ready messaging app built entirely on NIST-approved post-quantum cryptography.**  
No passwords. No accounts. No phone numbers. Just math.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Node.js](https://img.shields.io/badge/Node.js-20+-339933?logo=nodedotjs&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5+-3178C6?logo=typescript&logoColor=white)
![PostQuantum](https://img.shields.io/badge/Post--Quantum-ML--KEM%20%7C%20ML--DSA-blueviolet)

<br/>

<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-36-13-32_fc0d678334bc258baed0b03034397354.jpg" width="220" alt="Key Generation"/>
&nbsp;&nbsp;
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-34-25-20_fc0d678334bc258baed0b03034397354.jpg" width="220" alt="Biometric Lock Light"/>
&nbsp;&nbsp;
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-27-27-29_fc0d678334bc258baed0b03034397354.jpg" width="220" alt="Chat Dark"/>

<sub>Key generation → Biometric vault → End-to-end encrypted chat</sub>

</div>

---

## 🧠 The Problem We Solve

> **"Harvest now, decrypt later"** — adversaries are already storing encrypted traffic today, waiting for quantum computers to break RSA and ECC in the near future.

Current messaging apps (Signal, WhatsApp, Telegram) rely on classical cryptography (Curve25519, RSA) that **will be broken** by sufficiently powerful quantum computers. Quantum Safe is built from the ground up with **NIST FIPS 203/204 approved post-quantum algorithms**, ensuring your messages stay private — today, tomorrow, and in the quantum era.

### What Makes This Different

| | Traditional Messengers | Quantum Safe |
|---|---|---|
| **Key Exchange** | X25519 (vulnerable to quantum) | ML-KEM-768 + X25519 hybrid |
| **Signatures** | Ed25519 only | ML-DSA + Ed25519 dual signing |
| **Identity** | Phone number / email | SHA-256 cryptographic fingerprint |
| **Authentication** | Password + OTP | Biometric + zero-knowledge proof |
| **Local Storage** | Plaintext SQLite | SQLCipher AES-256 vault |
| **Server Knowledge** | Metadata visible | True zero-knowledge relay |

---

## 📸 App Screenshots

<div align="center">

### ☀️ Light Theme
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-32-05-72_fc0d678334bc258baed0b03034397354.jpg" width="180"/>
&nbsp;
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-32-11-58_fc0d678334bc258baed0b03034397354.jpg" width="180"/>
&nbsp;
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-27-33-49_fc0d678334bc258baed0b03034397354.jpg" width="180"/>
&nbsp;
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-32-19-08_fc0d678334bc258baed0b03034397354.jpg" width="180"/>

<sub>Home · Contacts · Chat · Profile with QR Identity</sub>

### 🌙 Dark Theme
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-27-49-14_fc0d678334bc258baed0b03034397354.jpg" width="180"/>
&nbsp;
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-33-11-40_fc0d678334bc258baed0b03034397354.jpg" width="180"/>
&nbsp;
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-27-27-29_fc0d678334bc258baed0b03034397354.jpg" width="180"/>
&nbsp;
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-33-24-66_fc0d678334bc258baed0b03034397354.jpg" width="180"/>

<sub>Home · Contacts · Chat · Profile with QR Identity</sub>

### 🔐 Security Features
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-34-12-07_fc0d678334bc258baed0b03034397354.jpg" width="180"/>
&nbsp;
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-36-13-32_fc0d678334bc258baed0b03034397354.jpg" width="180"/>
&nbsp;
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-27-53-41_fc0d678334bc258baed0b03034397354.jpg" width="180"/>
&nbsp;
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-22-12-38-29_fc0d678334bc258baed0b03034397354.jpg" width="180"/>

<sub>Biometric Lock (Dark) · Key Generation · QR Scanner · QR Scan Contact Add</sub>

### 📱 Additional Views
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-43-05-77_fc0d678334bc258baed0b03034397354.jpg" width="180"/>
&nbsp;
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-32-39-20_fc0d678334bc258baed0b03034397354.jpg" width="180"/>
&nbsp;
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-33-41-37_fc0d678334bc258baed0b03034397354.jpg" width="180"/>
&nbsp;
<img src="Quantum%20Safe%20images/Screenshot_2026-04-29-21-32-25-17_fc0d678334bc258baed0b03034397354.jpg" width="180"/>

<sub>Empty State · Add Contact (Manual) · Start Conversation Sheet · Crypto Info</sub>

</div>

---

## 🏗️ System Architecture

```mermaid
graph TB
    subgraph Client["📱 Android Client — Kotlin + Jetpack Compose"]
        UI["🎨 Glassmorphic UI<br/>Compose + Material3"]
        VM["📦 ViewModels<br/>Hilt DI + StateFlow"]
        Crypto["🔐 Crypto Engine<br/>liboqs · Bouncy Castle · Tink"]
        DB["💾 Room + SQLCipher<br/>AES-256 Encrypted Vault"]
        Net["🌐 Network Layer<br/>Ktor · Socket.io Client"]
        WM["⏱️ WorkManager<br/>Offline Queue"]
        Bio["👁️ BiometricPrompt<br/>TEE-backed Auth"]
    end

    subgraph Server["🚀 Express Backend — Node.js + TypeScript"]
        API["📡 REST API<br/>/auth · /keys · /health"]
        WS["⚡ Socket.io<br/>Real-time Relay"]
        MW["🛡️ Middleware<br/>Auth · Rate Limit · Helmet"]
        SVC["⚙️ Services<br/>FCM · OfflineQueue"]
    end

    subgraph Infra["☁️ Cloud Infrastructure"]
        Supa["🐘 Supabase PostgreSQL<br/>Users · Keys · FCM Tokens"]
        Redis["⚡ Upstash Redis<br/>Offline Queue · Pub/Sub"]
        FCM["🔔 Firebase FCM<br/>Push Notifications"]
    end

    UI --> VM --> Net
    VM --> Crypto
    VM --> DB
    Net -->|HTTPS + WSS| API
    Net -->|WebSocket| WS
    Bio --> DB
    WM --> Net

    API --> MW --> SVC
    WS --> SVC
    SVC --> Supa
    SVC --> Redis
    SVC --> FCM

    style Client fill:#1a1a2e,stroke:#e94560,color:#fff
    style Server fill:#16213e,stroke:#0f3460,color:#fff
    style Infra fill:#0f3460,stroke:#533483,color:#fff
```

### Layered Architecture (Android)

```
┌─────────────────────────────────────────────┐
│  PRESENTATION    Jetpack Compose + Material3 │
├─────────────────────────────────────────────┤
│  VIEWMODEL       Hilt + StateFlow + LiveData │
├─────────────────────────────────────────────┤
│  DOMAIN          Use Cases & Business Logic  │
├─────────────────────────────────────────────┤
│  REPOSITORY      Room + Network Abstraction  │
├─────────────────────────────────────────────┤
│  CRYPTO          liboqs + Bouncy Castle      │
├─────────────────────────────────────────────┤
│  NETWORK         Ktor + Socket.io Client     │
├─────────────────────────────────────────────┤
│  STORAGE         Room DB + SQLCipher (AES)   │
└─────────────────────────────────────────────┘
```

---

## 🔐 Encryption Deep Dive

This is the core innovation of Quantum Safe — a **hybrid post-quantum + classical** encryption pipeline that protects every message with multiple independent layers.

### Cryptographic Algorithms Used

| Layer | Algorithm | Standard | Purpose |
|-------|-----------|----------|---------|
| **Key Encapsulation** | ML-KEM-768 (CRYSTALS-Kyber) | NIST FIPS 203 | Quantum-resistant shared secret derivation |
| **Key Agreement** | X25519 (Curve25519 DH) | RFC 7748 | Classical fallback key exchange |
| **Digital Signature** | ML-DSA (CRYSTALS-Dilithium) | NIST FIPS 204 | Quantum-resistant message authentication |
| **Digital Signature** | Ed25519 (Edwards-curve DSA) | RFC 8032 | Classical fallback signature |
| **Symmetric Cipher** | AES-256-GCM | NIST SP 800-38D | Authenticated message encryption |
| **Key Derivation** | HMAC-SHA256 (HKDF) | RFC 5869 | Deterministic symmetric key from shared secret |
| **Identity Hash** | SHA-256 | FIPS 180-4 | Cryptographic fingerprint generation |
| **Database Encryption** | SQLCipher (AES-256-CBC) | Industry Standard | Local storage protection |
| **Transport** | TLS 1.3 | RFC 8446 | Network layer encryption |

### End-to-End Encryption Flow

```mermaid
sequenceDiagram
    participant Alice as 🔑 Alice (Sender)
    participant Server as 🖥️ Server (Blind Relay)
    participant Bob as 🔑 Bob (Receiver)

    Note over Alice: Compose: "Hello Bob!"

    rect rgb(40, 40, 80)
    Note over Alice: 🔒 ENCRYPTION PIPELINE
    Alice->>Alice: 1. Fetch Bob's ML-KEM public key
    Alice->>Alice: 2. ML-KEM Encapsulate → shared secret (32 bytes)
    Alice->>Alice: 3. HKDF(shared secret) → AES-256 key
    Alice->>Alice: 4. AES-256-GCM encrypt plaintext
    Alice->>Alice: 5. Sign with ML-DSA + Ed25519
    Alice->>Alice: 6. Pack into Protobuf envelope
    end

    Alice->>Server: 📦 Encrypted envelope (binary blob)

    Note over Server: ⛔ Cannot read payload<br/>Only sees: {to, from, timestamp}

    alt Bob is online
        Server->>Bob: ⚡ Socket.io → receive_message
    else Bob is offline
        Server->>Server: 📥 Queue in Redis (24h TTL)
        Server->>Bob: 🔔 FCM push (metadata only)
        Note over Bob: Opens app, reconnects
        Server->>Bob: 📤 Drain Redis queue
    end

    rect rgb(40, 80, 40)
    Note over Bob: 🔓 DECRYPTION PIPELINE
    Bob->>Bob: 1. Load private keys from SQLCipher vault
    Bob->>Bob: 2. ML-KEM Decapsulate → same shared secret
    Bob->>Bob: 3. HKDF(shared secret) → same AES-256 key
    Bob->>Bob: 4. Verify ML-DSA + Ed25519 signatures
    Bob->>Bob: 5. AES-256-GCM decrypt → "Hello Bob!" ✅
    end
```

### Why Hybrid Cryptography?

```
                    ┌──────────────────────────────────────┐
                    │      DEFENSE-IN-DEPTH STRATEGY       │
                    └──────────────────────────────────────┘

    ┌─────────────────────┐     ┌─────────────────────────┐
    │  POST-QUANTUM       │     │  CLASSICAL              │
    │  (New, conservative)│     │  (Battle-tested)        │
    │                     │     │                         │
    │  ML-KEM-768         │  +  │  X25519                 │
    │  (Key Encapsulation)│     │  (Key Agreement)        │
    │                     │     │                         │
    │  ML-DSA             │  +  │  Ed25519                │
    │  (Signatures)       │     │  (Signatures)           │
    └─────────────────────┘     └─────────────────────────┘
                    │                       │
                    └───────────┬───────────┘
                                │
                    ┌───────────▼───────────┐
                    │  BOTH must be broken  │
                    │  to compromise a msg  │
                    │                       │
                    │  Quantum computer?    │
                    │  → Classical holds.   │
                    │                       │
                    │  Classical break?     │
                    │  → PQ holds.          │
                    └───────────────────────┘
```

### Zero-Knowledge Identity Model

```
Traditional App                    Quantum Safe
─────────────                     ─────────────
Email: alice@mail.com             (nothing)
Password: ********               (nothing)
Phone: +1-555-1234                (nothing)
SMS OTP: 482901                   (nothing)
                                        │
                                        ▼
                               ┌────────────────────┐
                               │ On first launch:   │
                               │                    │
                               │ Generate:          │
                               │ • ML-KEM key pair  │
                               │ • X25519 key pair  │
                               │ • Ed25519 key pair │
                               │ • ML-DSA key pair  │
                               │                    │
                               │ Derive identity:   │
                               │ SHA-256(pubkeys)   │
                               │ = 64-char hex      │
                               │                    │
                               │ That's your ID.    │
                               │ No email.          │
                               │ No phone.          │
                               │ No password.       │
                               │ Ever.              │
                               └────────────────────┘
```

---

## 🔒 Security Architecture — 8 Layers Deep

| Layer | Protection | Implementation |
|-------|-----------|----------------|
| **1. Transport** | Wire encryption | TLS 1.3 / WSS · Helmet CSP · HSTS |
| **2. Authentication** | Identity verification | Zero-knowledge fingerprint (SHA-256 of public keys) |
| **3. Message Encryption** | Content confidentiality | ML-KEM-768 encapsulation → HKDF → AES-256-GCM |
| **4. Message Integrity** | Tamper detection | Dual signatures: ML-DSA + Ed25519 |
| **5. Data at Rest** | Device storage | Room + SQLCipher (AES-256) · Keys never leave device |
| **6. Biometric Gate** | Physical access control | BiometricPrompt + Android TEE · Fingerprint / Face |
| **7. Rate Limiting** | DDoS protection | 100 req/15min global · 10 req/min for uploads |
| **8. Push Privacy** | Notification metadata | Zero-knowledge FCM — no message content in push payload |

---

## 🛠️ Tech Stack

### Android Client

| Category | Technology | Details |
|----------|-----------|---------|
| **Language** | Kotlin 2.0+ | Coroutines, Flow, KSP |
| **UI Framework** | Jetpack Compose | Material Design 3 + Glassmorphism |
| **Architecture** | MVVM + Clean Architecture | Hilt DI · ViewModel · StateFlow |
| **Post-Quantum Crypto** | liboqs (OpenSSL) | ML-KEM-768, ML-DSA |
| **Classical Crypto** | Bouncy Castle | X25519, Ed25519 |
| **Symmetric Crypto** | Google Tink | AES-256-GCM |
| **Database** | Room + SQLCipher | AES-256 encrypted local storage |
| **Biometrics** | BiometricPrompt API | TEE-backed fingerprint/face |
| **Networking** | Ktor Client | HTTP + JSON serialization |
| **Real-time** | Socket.io Client | WebSocket messaging |
| **Serialization** | Protobuf + Kotlinx JSON | Binary message envelopes |
| **Background Tasks** | WorkManager | Offline message queuing |
| **Push** | Firebase Cloud Messaging | Silent data notifications |
| **Target SDK** | API 36 (Android 15) | Min SDK: API 26 (Android 8.0) |

### Backend Server

| Category | Technology | Details |
|----------|-----------|---------|
| **Runtime** | Node.js 20+ | TypeScript 6.0+ |
| **Framework** | Express.js 5.x | REST API + middleware |
| **Real-time** | Socket.io 4.x | Redis-adapted for horizontal scaling |
| **Database** | Supabase PostgreSQL | Users, public keys, FCM tokens |
| **Cache / Queue** | Upstash Redis (ioredis) | Offline message queue (24h TTL) |
| **Push** | Firebase Admin SDK | Zero-knowledge FCM notifications |
| **Validation** | Zod 4.x | Schema-based input validation |
| **Security** | Helmet + CORS + Rate Limit | HTTP hardening + DDoS protection |
| **Deployment** | Docker + Docker Compose | Containerized production deploy |

### Infrastructure

```
┌──────────────────────────────────────────────────────────┐
│                  PRODUCTION STACK                         │
│                                                          │
│   📱 Android Devices                                     │
│       ↕ HTTPS + WSS (TLS 1.3)                           │
│   🚀 Express Backend (Render / Docker)                   │
│       ↕           ↕              ↕                       │
│   🐘 Supabase   ⚡ Upstash     🔔 Firebase              │
│   PostgreSQL     Redis          FCM                      │
│   (Users/Keys)  (Msg Queue)    (Push)                    │
└──────────────────────────────────────────────────────────┘
```

---

## 🗄️ Database Schema

```mermaid
erDiagram
    USERS {
        text fingerprint PK "SHA-256(mlKemPK || x25519PK)"
        text ml_kem_public_key "Base64, 1184 bytes"
        text x25519_public_key "Base64, 32 bytes"
        timestamp created_at
    }

    PUBLIC_KEYS {
        uuid id PK
        text user_id FK "→ users.fingerprint"
        varchar algorithm "hybrid-pq"
        jsonb key_data "x25519PK, mlKemPK, ed25519Sig, mlDsaSig"
        timestamp created_at
        timestamp updated_at
    }

    FCM_TOKENS {
        text fingerprint PK_FK "→ users.fingerprint"
        text fcm_token "Firebase device token"
        timestamp updated_at
    }

    USERS ||--o| PUBLIC_KEYS : "has key bundle"
    USERS ||--o| FCM_TOKENS : "has device token"
```

---

## 🚀 Getting Started

### Prerequisites

| Requirement | Backend | Android |
|-------------|---------|---------|
| **Runtime** | Node.js 20+ | Android Studio Koala+ |
| **Language** | TypeScript | Kotlin 2.0+ / JDK 17+ |
| **Services** | Supabase · Upstash Redis · Firebase | Google Play Services |
| **SDK** | — | Target SDK 36 · Min SDK 26 |

### 1. Clone the Repository

```bash
git clone https://github.com/AsyncNigam/Quantum-safe-messenger.git
cd Quantum-safe-messenger
```

### 2. Backend Setup

```bash
cd Backend
npm install
```

Create a `.env` file (see `.env.example`):

```env
PORT=3000
NODE_ENV=development

# Supabase
SUPABASE_PROJECT_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_KEY=your-service-key

# Upstash Redis
REDIS_URL=redis://default:password@your-instance.upstash.io:12345

# Firebase
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
FIREBASE_CLIENT_EMAIL=firebase-adminsdk@your-project.iam.gserviceaccount.com
```

```bash
# Development (watch mode)
npm run dev

# Production
npm run build && npm run start:prod
```

### 3. Android Setup

1. Open `Android App/` in Android Studio
2. Place `google-services.json` in `Android App/app/`
3. Update the backend URL in the config
4. Build & run:

```bash
cd "Android App"
./gradlew assembleDebug
./gradlew installDebug
```

### 4. First Launch Experience

```
1. 🔐 Biometric authentication prompt (fingerprint/face)
2. 🔑 Automatic key generation (ML-KEM, X25519, Ed25519, ML-DSA)
3. 📡 Zero-knowledge registration with server
4. 📱 Home screen — ready to add contacts via QR scan
```

---

## 🔌 API Reference

### Authentication

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/auth/register` | — | Register with public keys (zero-knowledge) |
| `GET` | `/auth/lookup/:fingerprint` | Bearer | Lookup a contact's public keys |
| `POST` | `/auth/fcm-token` | Bearer | Register device push token |

### Key Management

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/keys/upload` | Bearer | Upload hybrid key bundle |
| `GET` | `/api/keys?page=1&limit=20` | — | Paginated public key discovery |

### Socket.io Events

| Event | Direction | Payload |
|-------|-----------|---------|
| `send_message` | Client → Server | `{ to: fingerprint, payload: Buffer }` |
| `receive_message` | Server → Client | `{ from: fingerprint, payload: Buffer, sentAt: ISO }` |

### Health Check

```
GET /health → { "status": "ok", "timestamp": "..." }
```

---

## 🐳 Deployment

### Docker Compose

```bash
docker-compose up -d
```

```yaml
services:
  backend:
    build: ./Backend
    ports: ["3000:3000"]
    depends_on: [redis]

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes --maxmemory 512mb
```

### Production Checklist

- [ ] `NODE_ENV=production`
- [ ] HTTPS/TLS on all endpoints
- [ ] CORS origins restricted
- [ ] Redis persistence enabled (AOF)
- [ ] Firebase service account secured
- [ ] Rate limiting configured
- [ ] Socket.io Redis adapter for horizontal scaling
- [ ] Supabase RLS policies active

---

## ✨ Key Features at a Glance

| Feature | Description |
|---------|-------------|
| 🧬 **Hybrid PQC Encryption** | ML-KEM + X25519 key exchange · ML-DSA + Ed25519 signatures |
| 🔑 **Zero-Knowledge Identity** | No email, phone, or password — identity derived from keys |
| 💎 **Glassmorphism UI** | Frosted glass aesthetic with adaptive light/dark themes |
| 👁️ **Biometric Vault** | Fingerprint/Face unlock guards SQLCipher encrypted database |
| 📴 **Offline-First** | WorkManager queues messages; Redis stores with 24h TTL |
| 🔔 **Silent Push** | Zero-knowledge FCM — push contains no message content |
| 📷 **QR Contact Exchange** | Scan a QR code to securely add contacts — no numbers needed |
| 🔄 **Real-Time Sync** | Socket.io with Redis pub/sub adapter for instant delivery |

---

## 🏆 Unique Innovations

| Innovation | How It Works | Why It Matters |
|-----------|-------------|----------------|
| **Opaque Relay Server** | Server only sees `{to, from, encrypted_blob}` | True zero-knowledge — no metadata leaks |
| **Dual-Algorithm Signing** | Every message signed by both ML-DSA and Ed25519 | Both must be broken to forge a message |
| **Per-Message KEM** | Fresh ML-KEM encapsulation per message | Forward secrecy — past messages safe even if keys leak |
| **Fingerprint Identity** | `SHA-256(mlKemPK ‖ x25519PK)` = your identity | Deterministic, verifiable, no central authority |
| **Biometric-Gated Crypto** | Private keys locked behind TEE biometric check | Physical access required to decrypt |

---

## 📖 Further Documentation

| Document | Description |
|----------|-------------|
| [Architecture Diagrams](Backend/ARCHITECTURE_DIAGRAMS.md) | Detailed backend architecture |
| [API Endpoints](Backend/ENDPOINTS.md) | Full endpoint reference with examples |
| [Database Schema](Backend/AUTH_DATABASE_QUICK_ANSWER.md) | PostgreSQL table definitions |
| [Authentication Flow](Backend/AUTHENTICATION_DATABASE_FLOW.md) | Registration & auth deep dive |
| [Socket.io Events](Backend/YOUR_SPECIFIC_ANSWERS.md) | Real-time messaging protocol |

---

## 🤝 Contributing

```bash
# 1. Fork & clone
git clone https://github.com/<your-username>/Quantum-safe-messenger.git

# 2. Create a feature branch
git checkout -b feature/amazing-feature

# 3. Commit and push
git commit -m "Add amazing feature"
git push origin feature/amazing-feature

# 4. Open a Pull Request
```

**Guidelines:** TypeScript for backend · Kotlin for Android · Tests for new features · Update docs

---

## 📝 License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE) for details.

---

<div align="center">

### Architected & Built by **Nigam Prasad Sahoo**

*Quantum-safe messaging for the post-quantum era*

<br/>

**🔐 Privacy by Math** · **🚀 Built for Tomorrow** · **🌍 Open Source**

⭐ Star this repo if you believe in post-quantum security!

</div>
