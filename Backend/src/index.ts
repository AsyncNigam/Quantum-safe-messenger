import 'dotenv/config';
import http from 'http';
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import { Server as SocketIOServer } from 'socket.io';
import { createAdapter } from '@socket.io/redis-adapter';

import { appConfig }                                        from './config/env';
import { pubClient, subClient, connectRedis }                from './config/redis';
import { errorHandler, generalLimiter, socketAuthMiddleware } from './api/middlewares';
import { socketController }                                  from './api/controllers';
import { authRoutes, keyRoutes, healthRoutes }                from './api/routes';

// ─── Express ──────────────────────────────────────────────────────────────────

const app = express();

// Security headers (CSP, HSTS, X-Frame-Options, etc.)
app.use(helmet());

app.use(cors({ origin: appConfig.clientOrigin }));

// Global rate limiter: 100 req / 15 min per IP
app.use(generalLimiter);

// Body parsers
app.use(express.json());
app.use(express.raw({ type: 'application/octet-stream', limit: '5mb' }));

// ─── Routes ───────────────────────────────────────────────────────────────────

app.use('/health', healthRoutes);
app.use('/api/auth',   authRoutes);   // ZK identity registration
app.use('/api/keys',   keyRoutes);    // Post-quantum key bundles

// ─── Global Error Handler ─────────────────────────────────────────────────────

app.use(errorHandler);

// ─── Bootstrap ────────────────────────────────────────────────────────────────

async function bootstrap(): Promise<void> {
  // ─── HTTP Server ────────────────────────────────────────────────────────────
  const httpServer = http.createServer(app);

  // ─── Socket.io ──────────────────────────────────────────────────────────────
  const io = new SocketIOServer(httpServer, {
    cors: {
      origin:  appConfig.clientOrigin,
      methods: ['GET', 'POST'],
    },
  });

  // Try to connect Redis — Socket.io adapter is optional
  const redisOk = await connectRedis();
  if (redisOk) {
    io.adapter(createAdapter(pubClient, subClient));
    console.log('[Socket.io] Redis adapter attached.');
  } else {
    console.warn('[Socket.io] Running WITHOUT Redis adapter (single-instance only).');
  }

  // Fingerprint authentication on every socket connection
  io.use(socketAuthMiddleware);

  // Delegate all connection/event logic to SocketController
  io.on('connection', (socket) => socketController.handleConnection(io, socket));

  // ─── Graceful Shutdown ──────────────────────────────────────────────────────

  const gracefulShutdown = async (signal: string): Promise<void> => {
    console.log(`\n[Shutdown] ${signal} received — shutting down gracefully…`);

    httpServer.close(() => console.log('[Shutdown] HTTP server closed.'));

    if (redisOk) {
      try {
        await Promise.all([pubClient.quit(), subClient.quit()]);
        console.log('[Shutdown] Redis clients disconnected.');
      } catch (err) {
        console.error('[Shutdown] Redis disconnect error:', (err as Error).message);
      }
    }

    console.log('[Shutdown] Complete. Exiting.');
    process.exit(0);
  };

  process.on('SIGTERM', () => void gracefulShutdown('SIGTERM'));
  process.on('SIGINT',  () => void gracefulShutdown('SIGINT'));

  // ─── Start ──────────────────────────────────────────────────────────────────

  httpServer.listen(appConfig.port, '0.0.0.0', () => {
    console.log(`✅  Quantum Messenger API  →  http://localhost:${appConfig.port}`);
    console.log(`🔌  Socket.io              →  ws://localhost:${appConfig.port}`);
    console.log(`🛡️  Helmet + Rate Limiting  →  active`);
    console.log(`🔐  Socket JWT Auth         →  active`);
    console.log(`🛑  Press Ctrl+C to stop`);
  });
}

bootstrap().catch((err) => {
  console.error('[Fatal] Bootstrap failed:', err);
  process.exit(1);
});

export default app;
