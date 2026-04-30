import 'dotenv/config';
import http from 'http';
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import { Server as SocketIOServer } from 'socket.io';
import { createAdapter } from '@socket.io/redis-adapter';

import { appConfig }                                        from './config/env';
import { pubClient, subClient, connectRedis }                from './config/redis';
import { initFirebase }                                      from './config/firebase';
import { errorHandler, generalLimiter, socketAuthMiddleware } from './api/middlewares';
import { socketController }                                  from './api/controllers';
import { authRoutes, keyRoutes, healthRoutes }                from './api/routes';

const app = express();

app.use(helmet());
app.use(cors({ origin: appConfig.clientOrigin }));

// Health check MUST be before the rate limiter — Render's health checker
// and cron pings hit this frequently and must never be rate-limited.
app.use('/health', healthRoutes);

// Rate limiter applied to all other routes
app.use(generalLimiter);

app.use(express.json());
app.use(express.raw({ type: 'application/octet-stream', limit: '5mb' }));

app.use('/api/auth',   authRoutes);
app.use('/api/keys',   keyRoutes);

app.use(errorHandler);

async function bootstrap(): Promise<void> {
  initFirebase();

  const httpServer = http.createServer(app);

  const io = new SocketIOServer(httpServer, {
    cors: {
      origin:  appConfig.clientOrigin,
      methods: ['GET', 'POST'],
    },
  });

  const redisOk = await connectRedis();
  if (redisOk) {
    io.adapter(createAdapter(pubClient, subClient));
    console.log('[Socket.io] Redis adapter attached.');
  } else {
    console.warn('[Socket.io] Running WITHOUT Redis adapter (single-instance only).');
  }

  io.use(socketAuthMiddleware);
  io.on('connection', (socket) => socketController.handleConnection(io, socket));

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

  httpServer.listen(appConfig.port, '0.0.0.0', () => {
    console.log(`✅  Quantum Messenger API  →  http://localhost:${appConfig.port}`);
    console.log(`🔌  Socket.io              →  ws://localhost:${appConfig.port}`);
    console.log(`🛡️  Helmet + Rate Limiting  →  active`);
    console.log(`🔐  Socket JWT Auth         →  active`);
  });
}

bootstrap().catch((err) => {
  console.error('[Fatal] Bootstrap failed:', err);
  process.exit(1);
});

export default app;
