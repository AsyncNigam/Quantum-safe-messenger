import { Redis } from 'ioredis';
import { SupabaseClient } from '@supabase/supabase-js';

export class MessageService {
  /**
   * Injects the Redis client and Supabase client.
   */
  constructor(
    private readonly redisClient: Redis,
    private readonly db: SupabaseClient
  ) {}

  /**
   * Stores an encrypted binary payload in a Redis Hash or Supabase fallback.
   */
  async queueOfflineMessage(recipientId: string, payload: Buffer): Promise<void> {
    const key = `offline:messages:${recipientId}`;
    let redisSuccess = false;
    let messageId = 'unknown';
    let senderId = 'unknown';

    try {
      const envelope = JSON.parse(payload.toString());
      senderId = envelope.from || 'unknown';
      messageId = envelope.messageId || 'unknown';
    } catch (_) {}

    if (this.redisClient.status === 'ready') {
      try {
        const multi = this.redisClient.multi();
        multi.hset(key, messageId, payload);
        multi.expire(key, 86400); // 24 hours
        const results = await multi.exec();
        if (results && results.length > 0) {
          redisSuccess = true;
        }
      } catch (err) {
        console.warn(`[MessageService] Redis queue failed, falling back to Supabase:`, (err as Error).message);
      }
    }

    if (!redisSuccess) {
      console.log(`[MessageService] Queuing offline message in Supabase for ${recipientId.slice(0, 12)}…`);
      const { error } = await this.db
        .from('offline_messages')
        .insert({
          recipient_id: recipientId,
          sender_id: senderId,
          message_id: messageId,
          payload: payload.toString('base64'),
        });

      if (error) {
        throw new Error(`Failed to queue offline message in Supabase: ${error.message}`);
      }
    }
  }

  /**
   * Fetches all messages from both Redis and Supabase fallback. Does NOT delete them.
   */
  async retrieveOfflineMessages(userId: string): Promise<Buffer[]> {
    const messages: Buffer[] = [];

    // 1. Retrieve from Redis if available
    if (this.redisClient.status === 'ready') {
      try {
        const key = `offline:messages:${userId}`;
        const redisMsgs = await this.redisClient.hvalsBuffer(key);
        if (redisMsgs && redisMsgs.length > 0) {
          messages.push(...redisMsgs);
        }
      } catch (err) {
        console.warn(`[MessageService] Redis retrieve failed:`, (err as Error).message);
      }
    }

    // 2. Retrieve from Supabase fallback
    try {
      const { data, error } = await this.db
        .from('offline_messages')
        .select('payload')
        .eq('recipient_id', userId)
        .order('created_at', { ascending: true });

      if (error) {
        console.error(`[MessageService] Supabase fallback retrieve failed:`, error.message);
      } else if (data && data.length > 0) {
        console.log(`[MessageService] Retrieved ${data.length} offline message(s) from Supabase fallback for ${userId.slice(0, 12)}…`);
        for (const row of data) {
          if (row.payload) {
            messages.push(Buffer.from(row.payload, 'base64'));
          }
        }
      }
    } catch (err) {
      console.error(`[MessageService] Supabase retrieve query failed:`, (err as Error).message);
    }

    return messages;
  }

  /**
   * Deletes a specific message from Redis and Supabase once it is acknowledged by the client.
   */
  async deleteOfflineMessage(userId: string, messageId: string): Promise<void> {
    // 1. Try deleting from Redis
    if (this.redisClient.status === 'ready') {
      try {
        const key = `offline:messages:${userId}`;
        await this.redisClient.hdel(key, messageId);
      } catch (err) {
        console.warn(`[MessageService] Redis delete failed for message ${messageId}:`, (err as Error).message);
      }
    }

    // 2. Try deleting from Supabase
    try {
      const { error } = await this.db
        .from('offline_messages')
        .delete()
        .eq('recipient_id', userId)
        .eq('message_id', messageId);

      if (error) {
        console.error(`[MessageService] Supabase fallback delete failed for message ${messageId}:`, error.message);
      }
    } catch (err) {
      console.error(`[MessageService] Supabase delete query failed:`, (err as Error).message);
    }
  }
}
