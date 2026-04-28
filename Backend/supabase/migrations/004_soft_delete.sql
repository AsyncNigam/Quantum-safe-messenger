-- Migration: Add soft-delete support to users table
-- Adds deleted_at column and allows public key columns to be nullable
-- (wiped on account deletion for security)

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS deleted_at timestamptz DEFAULT NULL;

-- Allow public key columns to be nullable (set to NULL on soft-delete)
ALTER TABLE users
  ALTER COLUMN ml_kem_public_key DROP NOT NULL;

ALTER TABLE users
  ALTER COLUMN x25519_public_key DROP NOT NULL;

-- Index for efficient lookup of active vs deleted users
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at);
