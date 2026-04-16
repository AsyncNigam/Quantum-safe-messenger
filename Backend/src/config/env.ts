// ─── Environment Configuration ────────────────────────────────────────────────
// Single source of truth for all environment variables.
// Fails fast at startup if required variables are missing.

export interface AppConfig {
  port: number;
  clientOrigin: string;
}

export interface SupabaseConfig {
  url: string;
  anonKey: string;
}

export interface RedisConfig {
  url: string;
}

/** Reads a required env var and terminates the process if absent. */
function requireEnv(key: string): string {
  const value = process.env[key];
  if (!value) {
    console.error(`[Fatal] Missing required environment variable: ${key}`);
    process.exit(1);
  }
  return value;
}

export const appConfig: AppConfig = {
  port: parseInt(process.env.PORT ?? '3000', 10),
  clientOrigin: process.env.CLIENT_ORIGIN ?? '*',
};

export const supabaseConfig: SupabaseConfig = {
  url: requireEnv('SUPABASE_URL'),
  anonKey: requireEnv('SUPABASE_ANON_KEY'),
};

export const redisConfig: RedisConfig = {
  url: process.env.REDIS_URL ?? 'redis://localhost:6379',
};
