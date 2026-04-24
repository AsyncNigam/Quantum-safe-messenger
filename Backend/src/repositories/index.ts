import { supabase, supabaseAdmin } from '../config/supabase';
import { KeyRepository } from './KeyRepository';

/**
 * Singleton KeyRepository instance.
 * Injects both public (supabase) and admin (supabaseAdmin) clients.
 */
export const keyRepository = new KeyRepository(supabase, supabaseAdmin);

// Export the class for type safety in other files
export * from './KeyRepository';
