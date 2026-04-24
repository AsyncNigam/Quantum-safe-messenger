import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { supabaseConfig } from './env';

/**
 * Public Supabase client using ANON_KEY.
 * Use this for authentication and user-facing queries.
 */
export const supabase: SupabaseClient = createClient(
  supabaseConfig.url,
  supabaseConfig.anonKey,
);

/**
 * Admin Supabase client using SERVICE_ROLE_KEY.
 * Use this for backend-to-database operations that bypass RLS.
 * ⚠️  Keep this secret - never expose to client.
 */
export const supabaseAdmin: SupabaseClient = createClient(
  supabaseConfig.url,
  supabaseConfig.serviceRoleKey,
);
