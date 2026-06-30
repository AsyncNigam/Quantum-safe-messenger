-- Migration 005: Create offline messages fallback table in Supabase
-- Used when Redis is down or unavailable.

CREATE TABLE IF NOT EXISTS offline_messages (
  id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  recipient_id text NOT NULL,
  sender_id text NOT NULL,
  payload text NOT NULL, -- base64 encoded JSON string
  created_at timestamp with time zone DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Index on recipient_id to speed up message lookup when user comes online
CREATE INDEX IF NOT EXISTS idx_offline_messages_recipient_id ON offline_messages(recipient_id);
