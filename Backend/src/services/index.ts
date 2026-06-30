import { storeClient } from '../config/redis';
import { supabaseAdmin } from '../config/supabase';
import { MessageService } from './MessageService';
import { keyService }     from './keyService';
import { FcmService }     from './fcmService';

/**
 * Singleton MessageService instance.
 * Receives the dedicated Redis store client and Supabase admin client.
 */
export const messageService = new MessageService(storeClient, supabaseAdmin);

/**
 * Singleton FcmService instance — handles push notifications.
 */
export const fcmService = new FcmService(supabaseAdmin);

/**
 * Re-export keyService (already a singleton object literal).
 */
export { keyService };

// Export classes for type safety in other files
export * from './MessageService';
export * from './keyService';
export * from './fcmService';
