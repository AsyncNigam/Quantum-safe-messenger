import { Redis } from 'ioredis';
export declare class MessageService {
    private readonly redisClient;
    /**
     * Injects the Redis client for data operations.
     */
    constructor(redisClient: Redis);
    /**
     * Stores an encrypted binary payload in a Redis list.
     * Sets a 24-hour TTL on the key.
     */
    queueOfflineMessage(recipientId: string, payload: Buffer): Promise<void>;
    /**
     * Fetches all messages from the list and deletes the key atomically.
     */
    retrieveAndClearOfflineMessages(userId: string): Promise<Buffer[]>;
}
//# sourceMappingURL=MessageService.d.ts.map