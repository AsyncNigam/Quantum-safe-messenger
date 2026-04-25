-- ─── FCM Token Storage ─────────────────────────────────────────────────────────
-- Stores Firebase Cloud Messaging tokens for push notifications.
-- One token per user (fingerprint). Updated on each app launch.

CREATE TABLE IF NOT EXISTS fcm_tokens (
    fingerprint TEXT PRIMARY KEY REFERENCES users(fingerprint) ON DELETE CASCADE,
    fcm_token   TEXT NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for quick token lookups by fingerprint
CREATE INDEX IF NOT EXISTS idx_fcm_tokens_fingerprint ON fcm_tokens(fingerprint);

-- RLS: Only service role can read/write (backend-only table)
ALTER TABLE fcm_tokens ENABLE ROW LEVEL SECURITY;
