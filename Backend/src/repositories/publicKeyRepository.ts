import { supabase } from '../config/supabase';
import { PublicKey, PaginatedResult } from '../models/publicKey';

export const publicKeyRepository = {
  /**
   * Returns a page of public keys ordered by creation date (newest first).
   * Throws on Supabase errors so the caller (service layer) can handle them.
   */
  async findPaginated(
    page: number,
    limit: number,
  ): Promise<PaginatedResult<PublicKey>> {
    const from = (page - 1) * limit;
    const to   = from + limit - 1;

    const { data, error, count } = await supabase
      .from('public_keys')
      .select('*', { count: 'exact' })
      .range(from, to)
      .order('created_at', { ascending: false });

    if (error) throw new Error(error.message);

    return {
      data:       (data as PublicKey[]) ?? [],
      page,
      limit,
      total:      count ?? 0,
      totalPages: Math.ceil((count ?? 0) / limit),
    };
  },
};
