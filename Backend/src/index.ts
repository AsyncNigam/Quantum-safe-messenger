import 'dotenv/config';
import http from 'http';
import express from 'express';
import cors from 'cors';
import { Server as SocketIOServer } from 'socket.io';
import { createAdapter } from '@socket.io/redis-adapter';

import { appConfig }            from './config/env';
import { pubClient, subClient } from './config/redis';
import { errorHandler }         from './api/middlewares/errorHandler';
import { registerSocketHandlers } from './services/socketService';
import healthRoutes             from './api/routes/healthRoutes';
import keyRoutes                from './api/routes/keyRoutes';

// ─── Express ──────────────────────────────────────────────────────────────────

const app = express();
app.use(cors({ origin: appConfig.clientOrigin }));
app.use(express.json());

// ─── HTTP Server ──────────────────────────────────────────────────────────────

const httpServer = http.createServer(app);

// ─── Socket.io ────────────────────────────────────────────────────────────────

const io = new SocketIOServer(httpServer, {
  cors: {
    origin:  appConfig.clientOrigin,
    methods: ['GET', 'POST'],
  },
});

// Redis adapter enables pub/sub across multiple server instances
io.adapter(createAdapter(pubClient, subClient));

// ─── Routes ───────────────────────────────────────────────────────────────────

app.use('/health', healthRoutes);
app.use('/keys',   keyRoutes);

// ─── Error Handler ────────────────────────────────────────────────────────────

app.use(errorHandler);

// ─── Socket Handlers ──────────────────────────────────────────────────────────

registerSocketHandlers(io);

// ─── Start ────────────────────────────────────────────────────────────────────

httpServer.listen(appConfig.port, () => {
  console.log(`✅ Quantum Messenger API  →  http://localhost:${appConfig.port}`);
  console.log(`🔌 Socket.io              →  ws://localhost:${appConfig.port}`);
});

export default app;
