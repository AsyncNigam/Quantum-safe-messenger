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
   * Stores an encrypted binary payload in a Redis list (FIFO order) or Supabase fallback.
   */
  async queueOfflineMessage(recipientId: string, payload: Buffer): Promise<void> {
    const key = `offline:messages:${recipientId}`;
    let redisSuccess = false;

    if (this.redisClient.status === 'ready') {
      try {
        const multi = this.redisClient.multi();
        multi.rpush(key, payload);
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
      let senderId = 'unknown';
      try {
        const envelope = JSON.parse(payload.toString());
        senderId = envelope.from || 'unknown';
      } catch (_) {}

      const { error } = await this.db
        .from('offline_messages')
        .insert({
          recipient_id: recipientId,
          sender_id: senderId,
          payload: payload.toString('base64'),
        });

      if (error) {
        throw new Error(`Failed to queue offline message in Supabase: ${error.message}`);
      }
    }
  }

  /**
   * Fetches all messages from both Redis and Supabase fallback, deletes them, and returns them.
   */
  async retrieveAndClearOfflineMessages(userId: string): Promise<Buffer[]> {
    const messages: Buffer[] = [];

    // 1. Retrieve from Redis if available
    if (this.redisClient.status === 'ready') {
      try {
        const key = `offline:messages:${userId}`;
        const multi = this.redisClient.multi();
        multi.lrangeBuffer(key, 0, -1);
        multi.del(key);
        const results = await multi.exec();
        if (results && results.length > 0) {
          const [err, redisMsgs] = results[0];
          if (!err && redisMsgs) {
            messages.push(...(redisMsgs as Buffer[]));
          }
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

        // Delete retrieved messages from Supabase fallback
        const { error: delError } = await this.db
          .from('offline_messages')
          .delete()
          .eq('recipient_id', userId);

        if (delError) {
          console.error(`[MessageService] Failed to clear Supabase fallback messages:`, delError.message);
        }
      }
    } catch (err) {
      console.error(`[MessageService] Supabase retrieve query failed:`, (err as Error).message);
    }

    return messages;
  }
}
