import { SupabaseClient } from '@supabase/supabase-js';
import { IUser } from '../models/User';

export class UserRepository {
  constructor(private readonly admin: SupabaseClient) {}

  /**
   * Upsert a user record keyed by fingerprint.
   * Idempotent — registering the same keys twice is safe.
   */
  async upsertUser(user: IUser): Promise<void> {
    const { error } = await this.admin
      .from('users')
      .upsert(
        {
          fingerprint:      user.fingerprint,
          ml_kem_public_key: user.mlKemPublicKey,
          x25519_public_key: user.x25519PublicKey,
          deleted_at:        null, // Re-registration clears soft-delete
        },
        { onConflict: 'fingerprint' },
      );

    if (error) {
      throw new Error(`Failed to upsert user: ${error.message}`);
    }
  }

  /**
   * Check whether a fingerprint exists in the registry.
   * Used by auth middlewares to validate anonymous identity tokens.
   */
  async findByFingerprint(fingerprint: string): Promise<IUser | null> {
    const { data, error } = await this.admin
      .from('users')
      .select('fingerprint, ml_kem_public_key, x25519_public_key, created_at, deleted_at')
      .eq('fingerprint', fingerprint)
      .maybeSingle();

    if (error) {
      throw new Error(`Failed to query user: ${error.message}`);
    }

    if (!data) return null;

    return {
      fingerprint:      data.fingerprint,
      mlKemPublicKey:   data.ml_kem_public_key,
      x25519PublicKey:  data.x25519_public_key,
      createdAt:        data.created_at,
      deletedAt:        data.deleted_at ?? null,
    };
  }

  /**
   * Soft-delete a user by setting deleted_at and wiping public keys.
   * The fingerprint row remains so contacts can detect "Deleted Account".
   */
  async softDeleteUser(fingerprint: string): Promise<void> {
    const { error } = await this.admin
      .from('users')
      .update({
        deleted_at:        new Date().toISOString(),
        ml_kem_public_key: null,
        x25519_public_key: null,
      })
      .eq('fingerprint', fingerprint);

    if (error) {
      throw new Error(`Failed to soft-delete user: ${error.message}`);
    }
  }
}
