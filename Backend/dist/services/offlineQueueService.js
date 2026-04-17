"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.offlineQueueService = void 0;
const redis_1 = require("../config/redis");
/** Redis list key for a user's offline message queue. */
const queueKey = (userId) => `offline_queue:${userId}`;
/** 24 hours — messages expire from Redis after this TTL. */
const OFFLINE_QUEUE_TTL_SECONDS = 86400;
exports.offlineQueueService = {
    /**
     * Appends an opaque message envelope to the recipient's Redis list and
     * refreshes the TTL to 24 hours.
     *
     * ZERO-KNOWLEDGE: the envelope's `payload` is serialised as-is via
     * JSON.stringify without any inspection or transformation.
     */
    async enqueue(recipientId, envelope) {
        const key = queueKey(recipientId);
        await redis_1.storeClient.rpush(key, JSON.stringify(envelope));
        await redis_1.storeClient.expire(key, OFFLINE_QUEUE_TTL_SECONDS);
        console.log(`[OfflineQueue] Queued | to=${recipientId} | ttl=${OFFLINE_QUEUE_TTL_SECONDS}s`);
    },
    /**
     * Reads all messages from the user's offline queue, emits each to their
     * socket, then atomically deletes the queue key.
     *
     * ZERO-KNOWLEDGE: the inner `payload` of each envelope is never inspected;
     * only the envelope wrapper is parsed for routing purposes.
     */
    async drain(userId, socket) {
        const key = queueKey(userId);
        const raw = await redis_1.storeClient.lrange(key, 0, -1);
        if (raw.length === 0)
            return;
        console.log(`[OfflineQueue] Draining ${raw.length} message(s) | userId=${userId}`);
        for (const item of raw) {
            try {
                const envelope = JSON.parse(item);
                socket.emit('receive_message', envelope);
            }
            catch {
                console.warn(`[OfflineQueue] Failed to parse queued item | userId=${userId}`);
            }
        }
        await redis_1.storeClient.del(key);
        console.log(`[OfflineQueue] Cleared | userId=${userId}`);
    },
};
//# sourceMappingURL=offlineQueueService.js.map