import Redis from 'ioredis';
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
export declare const pubClient: Redis;
export declare const subClient: Redis;
export declare const storeClient: Redis;
//# sourceMappingURL=redis.d.ts.map