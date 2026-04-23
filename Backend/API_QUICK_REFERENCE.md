# Quick Reference - API Endpoints Summary

## REST Endpoints

| Method | Path | Auth | Rate Limit | Purpose |
|--------|------|------|-----------|---------|
| `GET` | `/health` | ❌ No | 100/15min | Health check |
| `GET` | `/keys/sync?page=1&limit=20` | ❌ No | 100/15min | Get public keys for peer discovery |
| `POST` | `/keys/upload` | ✅ JWT | 10/min | Upload hybrid key bundle |

## WebSocket Events

| Event | Direction | Auth | Purpose |
|-------|-----------|------|---------|
| `send_message` | Client → Server | ✅ JWT | Send message to recipient |
| `receive_message` | Server → Client | ✅ JWT | Receive message from sender |
| `disconnect` | Automatic | - | Connection closed |

## Key Features

- **Authentication:** Supabase JWT
- **Message Queue:** Redis (offline message storage)
- **Scaling:** Redis pub/sub adapter for Socket.io
- **Security:** Helmet + CORS + Rate Limiting
- **Crypto:** Hybrid classical + post-quantum cryptography

## Build Status

✅ **TypeScript Compilation:** Successful (no errors)

## Server Connection

```
URL: http://localhost:3000
WebSocket: ws://localhost:3000
Port: 3000 (default, configurable)
```

## Example Commands

```bash
# Health check
curl http://localhost:3000/health

# Get keys (no auth)
curl "http://localhost:3000/keys/sync?page=1&limit=20"

# Upload keys (with JWT)
curl -X POST http://localhost:3000/keys/upload \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

---

For detailed documentation, see **ENDPOINTS.md**
