import { SupabaseClient } from '@supabase/supabase-js';
import { IKeyBundle } from '../models/KeyBundle';
import { PublicKey, PaginatedResult } from '../models/publicKey';
export declare class KeyRepository {
    private readonly supabase;
    /**
     * Injects the Supabase client for database operations.
     */
    constructor(supabase: SupabaseClient);
    /**
     * Fetches a paginated list of public keys using PostgreSQL range.
     */
    findKeysPaginated(page: number, limit: number): Promise<PaginatedResult<PublicKey>>;
    /**
     * Inserts or upserts a user's hybrid public keys.
     */
    uploadKeys(keyData: IKeyBundle): Promise<void>;
}
//# sourceMappingURL=KeyRepository.d.ts.map