-- Migration 006: Add message_id to offline_messages table
-- This enables O(1) deletion of individual messages upon delivery acknowledgment
-- to support At-Least-Once Delivery.

ALTER TABLE offline_messages ADD COLUMN IF NOT EXISTS message_id text;

-- Create an index to speed up message deletion
CREATE INDEX IF NOT EXISTS idx_offline_messages_message_id ON offline_messages(message_id);
