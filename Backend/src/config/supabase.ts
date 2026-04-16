import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { supabaseConfig } from './env';

/**
 * Singleton Supabase client.
 * Import this instance anywhere database access is needed.
 */
export const supabase: SupabaseClient = createClient(
  supabaseConfig.url,
  supabaseConfig.anonKey,
);
