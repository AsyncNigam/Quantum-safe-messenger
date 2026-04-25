import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { supabaseConfig } from './env';

/**
 * Admin Supabase client using SERVICE_ROLE_KEY.
 *
 * Used exclusively for backend DB operations (users table, public_keys table).
 * The public anon client has been removed — Supabase Auth is no longer used.
 * Authentication is now handled via the cryptographic Text Fingerprint system.
 *
 * ⚠️  SERVICE_ROLE_KEY bypasses RLS — never expose to clients.
 */
export const supabaseAdmin: SupabaseClient = createClient(
  supabaseConfig.url,
  supabaseConfig.serviceRoleKey,
);
