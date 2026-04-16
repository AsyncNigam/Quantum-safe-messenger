import 'dotenv/config';
import http from 'http';
import express, { Application, Request, Response, NextFunction } from 'express';
import cors from 'cors';
import { Server as SocketIOServer, Socket } from 'socket.io';
import { createClient } from '@supabase/supabase-js';
import { createClient as createRedisClient } from 'ioredis';
import { createAdapter } from '@socket.io/redis-adapter';

// ─── Env Validation ──────────────────────────────────────────────────────────

const {
  PORT = '3000',
  SUPABASE_URL,
  SUPABASE_ANON_KEY,
  REDIS_URL = 'redis://localhost:6379',
  CLIENT_ORIGIN = '*',
} = process.env;

if (!SUPABASE_URL || !SUPABASE_ANON_KEY) {
  console.error('[Fatal] Missing SUPABASE_URL or SUPABASE_ANON_KEY in environment.');
  process.exit(1);
}

// ─── Supabase Client ─────────────────────────────────────────────────────────

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// ─── Redis Clients (pub/sub must be separate connections) ─────────────────────

const pubClient = createRedisClient(REDIS_URL);
const subClient = pubClient.duplicate();

pubClient.on('error', (err) => console.error('[Redis Pub] Error:', err.message));
subClient.on('error', (err) => console.error('[Redis Sub] Error:', err.message));

// ─── Express App ─────────────────────────────────────────────────────────────

const app: Application = express();

app.use(cors({ origin: CLIENT_ORIGIN }));
app.use(express.json());

// ─── HTTP Server (required to share port with Socket.io) ─────────────────────

const httpServer = http.createServer(app);

// ─── Socket.io Server ────────────────────────────────────────────────────────

const io = new SocketIOServer(httpServer, {
  cors: {
    origin: CLIENT_ORIGIN,
    methods: ['GET', 'POST'],
  },
});

// Attach Redis adapter for horizontal scaling across multiple server instances
io.adapter(createAdapter(pubClient, subClient));

// ─── Socket.io Connection Handler ────────────────────────────────────────────

/**
 * ZERO-KNOWLEDGE POLICY:
 *   This server acts purely as a relay. It never inspects, stores,
 *   decrypts, or modifies any message payload. All cryptographic
 *   operations happen exclusively on the client side.
 */
io.on('connection', (socket: Socket) => {
  // The client must pass their userId as a handshake query param
  const userId = socket.handshake.query.userId as string | undefined;

  if (!userId || typeof userId !== 'string' || userId.trim() === '') {
    console.warn(`[Socket] Connection rejected — missing userId. socket=${socket.id}`);
    socket.disconnect(true);
    return;
  }

  // Each user joins a private room identified by their userId.
  // Messages are routed to this room, never broadcast globally.
  socket.join(userId);
  console.log(`[Socket] Connected  | socket=${socket.id} | room=${userId}`);

  // ── send_message ──────────────────────────────────────────────────────────
  /**
   * Expected payload shape from the client:
   * {
   *   to:      string,  // recipientUserId — the target room
   *   payload: unknown, // opaque encrypted blob — NEVER inspected by the server
   * }
   *
   * The server only reads `to` for routing. The `payload` is forwarded
   * as-is, without any access, transformation, or logging of its contents.
   */
  socket.on('send_message', (envelope: { to: string; payload: unknown }) => {
    const { to, payload } = envelope;

    if (!to || typeof to !== 'string') {
      console.warn(`[Socket] send_message ignored — invalid 'to' field. socket=${socket.id}`);
      return;
    }

    // Log routing metadata only — payload contents are intentionally opaque
    console.log(
      `[Socket] Relaying message | from=${userId} | to=${to} | ` +
      `payloadType=${typeof payload} | timestamp=${new Date().toISOString()}`
    );

    // Relay the opaque encrypted payload to the recipient's private room
    io.to(to).emit('receive_message', {
      from: userId,
      payload,      // zero-knowledge: forwarded without inspection
      sentAt: new Date().toISOString(),
    });
  });

  // ── Disconnect ────────────────────────────────────────────────────────────

  socket.on('disconnect', (reason) => {
    console.log(`[Socket] Disconnected | socket=${socket.id} | room=${userId} | reason=${reason}`);
  });
});

// ─── REST Routes ──────────────────────────────────────────────────────────────

app.get('/health', (_req: Request, res: Response) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// GET /keys/sync — paginated public keys from Supabase
app.get('/keys/sync', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const page  = Math.max(1, parseInt((req.query.page  as string) || '1',  10));
    const limit = Math.min(100, Math.max(1, parseInt((req.query.limit as string) || '20', 10)));
    const from  = (page - 1) * limit;
    const to    = from + limit - 1;

    const { data, error, count } = await supabase
      .from('public_keys')
      .select('*', { count: 'exact' })
      .range(from, to)
      .order('created_at', { ascending: false });

    if (error) {
      console.error('[Supabase] /keys/sync error:', error.message);
      res.status(502).json({ error: 'Database error', details: error.message });
      return;
    }

    res.json({
      data,
      page,
      limit,
      total: count ?? 0,
      totalPages: Math.ceil((count ?? 0) / limit),
    });
  } catch (err) {
    next(err);
  }
});

// ─── Global Error Handler ─────────────────────────────────────────────────────

app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
  console.error('[Unhandled Error]', err.message);
  res.status(500).json({ error: 'Internal server error', details: err.message });
});

// ─── Start Server ─────────────────────────────────────────────────────────────

httpServer.listen(Number(PORT), () => {
  console.log(`✅ Quantum Messenger API  →  http://localhost:${PORT}`);
  console.log(`🔌 Socket.io              →  ws://localhost:${PORT}`);
  console.log(`🔴 Redis Adapter          →  ${REDIS_URL}`);
});

export default app;
