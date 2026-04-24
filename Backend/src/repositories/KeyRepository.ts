import { SupabaseClient } from '@supabase/supabase-js';
import { IKeyBundle } from '../models/KeyBundle';
import { PublicKey, PaginatedResult } from '../models/publicKey';

export class KeyRepository {
  /**
   * Injects two Supabase clients:
   * - supabase: public client (with anonKey) for READ operations
   * - supabaseAdmin: admin client (with service role key) for INSERT/UPDATE operations
   */
  constructor(
    private readonly supabase: SupabaseClient,
    private readonly supabaseAdmin: SupabaseClient
  ) {}

  /**
   * Fetches a paginated list of public keys using PostgreSQL range.
   */
  async findKeysPaginated(
    page: number,
    limit: number,
  ): Promise<PaginatedResult<PublicKey>> {
    const from = (page - 1) * limit;
    const to = from + limit - 1;

    const { data, error, count } = await this.supabase
      .from('public_keys')
      .select('*', { count: 'exact' })
      .range(from, to)
      .order('created_at', { ascending: false });

    if (error) {
      throw new Error(`Failed to fetch keys: ${error.message}`);
    }

    return {
      data: (data as PublicKey[]) ?? [],
      page,
      limit,
      total: count ?? 0,
      totalPages: Math.ceil((count ?? 0) / limit),
    };
  }

  /**
   * Inserts or upserts a user's hybrid public keys.
   * Uses the admin client to bypass RLS policies.
   */
  async uploadKeys(keyData: IKeyBundle): Promise<void> {
    const payload = {
      user_id: keyData.userId,
      algorithm: 'hybrid-pq',
      key_data: JSON.stringify({
        x25519PublicKey: keyData.x25519PublicKey,
        mlKemPublicKey: keyData.mlKemPublicKey,
        ed25519Signature: keyData.ed25519Signature,
        mlDsaSignature: keyData.mlDsaSignature,
      }),
    };

    // Use admin client to bypass RLS and insert/update user's keys
    const { error } = await this.supabaseAdmin
      .from('public_keys')
      .upsert(payload, { onConflict: 'user_id' });

    if (error) {
      throw new Error(`Failed to upload keys: ${error.message}`);
    }
  }
}
