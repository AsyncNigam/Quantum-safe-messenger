"use strict";
// ─── Environment Configuration ────────────────────────────────────────────────
// Single source of truth for all environment variables.
// Fails fast at startup if required variables are missing.
Object.defineProperty(exports, "__esModule", { value: true });
exports.redisConfig = exports.supabaseConfig = exports.appConfig = void 0;
/** Reads a required env var and terminates the process if absent. */
function requireEnv(key) {
    const value = process.env[key];
    if (!value) {
        console.error(`[Fatal] Missing required environment variable: ${key}`);
        process.exit(1);
    }
    return value;
}
exports.appConfig = {
    port: parseInt(process.env.PORT ?? '3000', 10),
    clientOrigin: process.env.CLIENT_ORIGIN ?? '*',
};
exports.supabaseConfig = {
    url: requireEnv('SUPABASE_URL'),
    anonKey: requireEnv('SUPABASE_ANON_KEY'),
};
exports.redisConfig = {
    url: process.env.REDIS_URL ?? 'redis://localhost:6379',
};
//# sourceMappingURL=env.js.map