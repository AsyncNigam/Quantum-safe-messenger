import Redis, { RedisOptions } from 'ioredis';
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
 *
 * maxRetriesPerRequest is set to 3 so the server doesn't hang when Redis
 * is unavailable. lazyConnect prevents blocking startup if Redis is down.
 */
const redisOptions: RedisOptions = {
  maxRetriesPerRequest: 3,
  retryStrategy(times: number) {
    if (times > 3) {
      // Stop retrying — server runs fine without Redis (no offline queue)
      return null;
    }
    return Math.min(times * 300, 2000);
  },
  lazyConnect: true,
};

export const pubClient   = new Redis(redisConfig.url, redisOptions);
export const subClient   = pubClient.duplicate();
export const storeClient = pubClient.duplicate();

/** Whether Redis connected successfully */
export let redisAvailable = false;

// Suppress noisy error logs after initial failure — only log once per client
let errorLogged = { pub: false, sub: false, store: false };
pubClient.on('error',   () => { if (!errorLogged.pub)   { errorLogged.pub = true; } });
subClient.on('error',   () => { if (!errorLogged.sub)   { errorLogged.sub = true; } });
storeClient.on('error', () => { if (!errorLogged.store) { errorLogged.store = true; } });

/**
 * Attempt to connect all Redis clients.
 * Returns true if connections succeeded, false otherwise.
 */
export async function connectRedis(): Promise<boolean> {
  try {
    await Promise.all([
      pubClient.connect(),
      subClient.connect(),
      storeClient.connect(),
    ]);
    redisAvailable = true;
    console.log('[Redis] ✅ All clients connected.');
    return true;
  } catch (err) {
    redisAvailable = false;
    console.warn('[Redis] ⚠️  Not available — offline message queue disabled.');
    return false;
  }
}

