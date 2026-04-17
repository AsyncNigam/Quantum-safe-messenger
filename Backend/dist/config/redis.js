"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.storeClient = exports.subClient = exports.pubClient = void 0;
const ioredis_1 = __importDefault(require("ioredis"));
const env_1 = require("./env");
/**
 * Three dedicated Redis connections:
 *
 *   pubClient   — used exclusively by the Socket.io Redis Adapter to publish events.
 *   subClient   — used exclusively by the Socket.io Redis Adapter to subscribe.
 *   storeClient — general-purpose data operations (offline queue, caching, etc.).
 *
 * pub and sub MUST be separate connections; sharing them with the adapter
 * would block all other commands while the adapter holds the connection in
 * subscribe mode.
 */
exports.pubClient = new ioredis_1.default(env_1.redisConfig.url);
exports.subClient = exports.pubClient.duplicate();
exports.storeClient = exports.pubClient.duplicate();
exports.pubClient.on('error', (err) => console.error('[Redis Pub]   Error:', err.message));
exports.subClient.on('error', (err) => console.error('[Redis Sub]   Error:', err.message));
exports.storeClient.on('error', (err) => console.error('[Redis Store] Error:', err.message));
//# sourceMappingURL=redis.js.map