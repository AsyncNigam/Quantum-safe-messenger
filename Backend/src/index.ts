import 'dotenv/config';
import http from 'http';
import express from 'express';
import cors from 'cors';
import { Server as SocketIOServer } from 'socket.io';
import { createAdapter } from '@socket.io/redis-adapter';

import { appConfig }            from './config/env';
import { pubClient, subClient, storeClient } from './config/redis';
import { errorHandler }         from './api/middlewares/errorHandler';
import healthRoutes             from './api/routes/healthRoutes';
import keyRoutes                from './api/routes/keyRoutes';

import { MessageService }       from './services/MessageService';
import { SocketController }     from './api/controllers/SocketController';

// ─── Express ──────────────────────────────────────────────────────────────────

const app = express();
app.use(cors({ origin: appConfig.clientOrigin }));
app.use(express.json());

// ─── HTTP Server ──────────────────────────────────────────────────────────────

const httpServer = http.createServer(app);

// ─── Socket.io & Controllers ──────────────────────────────────────────────────

const io = new SocketIOServer(httpServer, {
  cors: {
    origin:  appConfig.clientOrigin,
    methods: ['GET', 'POST'],
  },
});

// Redis adapter enables pub/sub across multiple server instances
io.adapter(createAdapter(pubClient, subClient));

// Instantiate Socket Controller and inject Message Service
const messageService = new MessageService(storeClient);
const socketController = new SocketController(messageService);

io.on('connection', (socket) => socketController.handleConnection(io, socket));

// ─── Routes ───────────────────────────────────────────────────────────────────

app.use('/health', healthRoutes);
app.use('/keys',   keyRoutes);

// ─── Error Handler ────────────────────────────────────────────────────────────

app.use(errorHandler);

// ─── Start ────────────────────────────────────────────────────────────────────

httpServer.listen(appConfig.port, () => {
  console.log(`✅ Quantum Messenger API  →  http://localhost:${appConfig.port}`);
  console.log(`🔌 Socket.io              →  ws://localhost:${appConfig.port}`);
});

export default app;
