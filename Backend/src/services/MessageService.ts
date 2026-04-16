import { Redis } from 'ioredis';

export class MessageService {
  /**
   * Injects the Redis client for data operations.
   */
  constructor(private readonly redisClient: Redis) {}

  /**
   * Stores an encrypted binary payload in a Redis list.
   * Sets a 24-hour TTL on the key.
   */
  async queueOfflineMessage(recipientId: string, payload: Buffer): Promise<void> {
    const key = `offline:messages:${recipientId}`;
    
    // Use multi() to queue and set TTL atomically
    const multi = this.redisClient.multi();
    
    // We use lpush to queue the buffer
    multi.lpush(key, payload);
    multi.expire(key, 86400); // 24 hours in seconds
    
    const results = await multi.exec();
    
    if (!results) {
      throw new Error(`Failed to execute Redis transaction for queueOfflineMessage`);
    }
  }

  /**
   * Fetches all messages from the list and deletes the key atomically.
   */
  async retrieveAndClearOfflineMessages(userId: string): Promise<Buffer[]> {
    const key = `offline:messages:${userId}`;
    const multi = this.redisClient.multi();
    
    // Use lrangeBuffer to ensure we receive Buffers back from Redis,
    // avoiding string decoding corruption for binary ciphertexts.
    multi.lrangeBuffer(key, 0, -1);
    multi.del(key);
    
    const results = await multi.exec();
    
    if (!results || results.length === 0) {
      return [];
    }

    // `results` is an array of [error, result] tuples for each queued command.
    // results[0] corresponds to `lrangeBuffer`.
    const [err, messages] = results[0];
    
    if (err) {
      throw err;
    }

    return (messages as Buffer[]) ?? [];
  }
}
