"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
require("dotenv/config");
const http_1 = __importDefault(require("http"));
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const helmet_1 = __importDefault(require("helmet"));
const socket_io_1 = require("socket.io");
const redis_adapter_1 = require("@socket.io/redis-adapter");
const env_1 = require("./config/env");
const redis_1 = require("./config/redis");
const middlewares_1 = require("./api/middlewares");
const controllers_1 = require("./api/controllers");
const routes_1 = require("./api/routes");
// ─── Express ──────────────────────────────────────────────────────────────────
const app = (0, express_1.default)();
// Security headers (CSP, HSTS, X-Frame-Options, etc.)
app.use((0, helmet_1.default)());
app.use((0, cors_1.default)({ origin: env_1.appConfig.clientOrigin }));
// Global rate limiter: 100 req / 15 min per IP
app.use(middlewares_1.generalLimiter);
// Body parsers
app.use(express_1.default.json());
app.use(express_1.default.raw({ type: 'application/octet-stream', limit: '5mb' }));
// ─── HTTP Server ──────────────────────────────────────────────────────────────
const httpServer = http_1.default.createServer(app);
// ─── Socket.io ────────────────────────────────────────────────────────────────
const io = new socket_io_1.Server(httpServer, {
    cors: {
        origin: env_1.appConfig.clientOrigin,
        methods: ['GET', 'POST'],
    },
});
// Horizontal scaling across instances via Redis pub/sub
io.adapter((0, redis_adapter_1.createAdapter)(redis_1.pubClient, redis_1.subClient));
// JWT authentication on every socket connection (before any event handler runs)
io.use(middlewares_1.socketAuthMiddleware);
// Delegate all connection/event logic to SocketController
io.on('connection', (socket) => controllers_1.socketController.handleConnection(io, socket));
// ─── Routes ───────────────────────────────────────────────────────────────────
app.use('/health', routes_1.healthRoutes);
app.use('/keys', routes_1.keyRoutes);
// ─── Global Error Handler ─────────────────────────────────────────────────────
app.use(middlewares_1.errorHandler);
// ─── Graceful Shutdown ────────────────────────────────────────────────────────
const gracefulShutdown = async (signal) => {
    console.log(`\n[Shutdown] ${signal} received — shutting down gracefully…`);
    httpServer.close(() => console.log('[Shutdown] HTTP server closed.'));
    try {
        await Promise.all([redis_1.pubClient.quit(), redis_1.subClient.quit()]);
        console.log('[Shutdown] Redis clients disconnected.');
    }
    catch (err) {
        console.error('[Shutdown] Redis disconnect error:', err.message);
    }
    console.log('[Shutdown] Complete. Exiting.');
    process.exit(0);
};
process.on('SIGTERM', () => void gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => void gracefulShutdown('SIGINT'));
// ─── Start ────────────────────────────────────────────────────────────────────
httpServer.listen(env_1.appConfig.port, () => {
    console.log(`✅  Quantum Messenger API  →  http://localhost:${env_1.appConfig.port}`);
    console.log(`🔌  Socket.io              →  ws://localhost:${env_1.appConfig.port}`);
    console.log(`🛡️  Helmet + Rate Limiting  →  active`);
    console.log(`🔐  Socket JWT Auth         →  active`);
    console.log(`🛑  Press Ctrl+C to stop`);
});
exports.default = app;
//# sourceMappingURL=index.js.map