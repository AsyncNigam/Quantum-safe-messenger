/**
 * Represents a registered anonymous user in the `users` Supabase table.
 * No PII is stored — only cryptographic material.
 */
export interface IUser {
  /** SHA-256(mlKemPublicKey_bytes || x25519PublicKey_bytes) — hex-encoded, 64 chars */
  fingerprint: string;
  mlKemPublicKey: string;   // Base64
  x25519PublicKey: string;  // Base64
  createdAt?: string;
  /** ISO timestamp when the account was soft-deleted, null if active */
  deletedAt?: string | null;
}
