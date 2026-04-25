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
      .select('fingerprint, ml_kem_public_key, x25519_public_key, created_at')
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
    };
  }
}
