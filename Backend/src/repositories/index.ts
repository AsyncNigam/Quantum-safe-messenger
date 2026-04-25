import { supabaseAdmin } from '../config/supabase';
import { KeyRepository }  from './KeyRepository';
import { UserRepository } from './UserRepository';

/**
 * Singleton KeyRepository — uses the admin client only (public client removed).
 * Reads also use the admin client since there is no row-level JWT to present.
 */
export const keyRepository  = new KeyRepository(supabaseAdmin, supabaseAdmin);

/**
 * Singleton UserRepository — fingerprint-based identity store.
 */
export const userRepository = new UserRepository(supabaseAdmin);

// Export classes for type safety in other files
export * from './KeyRepository';
export * from './UserRepository';
