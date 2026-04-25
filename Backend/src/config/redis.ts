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
 *
 * maxRetriesPerRequest is set to 3 so the server doesn't hang when Redis
 * is unavailable. lazyConnect prevents blocking startup if Redis is down.
 */
const redisOptions: Redis.RedisOptions = {
  maxRetriesPerRequest: 3,
  retryStrategy(times: number) {
    if (times > 5) {
      console.warn('[Redis] Giving up reconnection after 5 attempts.');
      return null;            // stop retrying
    }
    return Math.min(times * 200, 2000);
  },
  lazyConnect: true,
};

export const pubClient   = new Redis(redisConfig.url, redisOptions);
export const subClient   = pubClient.duplicate();
export const storeClient = pubClient.duplicate();

/** Whether Redis connected successfully */
export let redisAvailable = false;

pubClient.on('error',   (err: Error) => console.error('[Redis Pub]   Error:', err.message));
subClient.on('error',   (err: Error) => console.error('[Redis Sub]   Error:', err.message));
storeClient.on('error', (err: Error) => console.error('[Redis Store] Error:', err.message));

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
    console.warn('[Redis] ⚠️  Not available — running without Redis.', (err as Error).message);
    return false;
  }
}
