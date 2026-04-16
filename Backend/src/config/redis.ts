import Redis from 'ioredis';
import { redisConfig } from './env';

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
export const pubClient   = new Redis(redisConfig.url);
export const subClient   = pubClient.duplicate();
export const storeClient = pubClient.duplicate();

pubClient.on('error',   (err: Error) => console.error('[Redis Pub]   Error:', err.message));
subClient.on('error',   (err: Error) => console.error('[Redis Sub]   Error:', err.message));
storeClient.on('error', (err: Error) => console.error('[Redis Store] Error:', err.message));
