"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.KeyRepository = void 0;
class KeyRepository {
    /**
     * Injects the Supabase client for database operations.
     */
    constructor(supabase) {
        this.supabase = supabase;
    }
    /**
     * Fetches a paginated list of public keys using PostgreSQL range.
     */
    async findKeysPaginated(page, limit) {
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
            data: data ?? [],
            page,
            limit,
            total: count ?? 0,
            totalPages: Math.ceil((count ?? 0) / limit),
        };
    }
    /**
     * Inserts or upserts a user's hybrid public keys.
     */
    async uploadKeys(keyData) {
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
        // Upsert the record for the user. Requires 'user_id' to be a unique constraint
        // in the 'public_keys' table if onConflict is used. We assume standard upsert behavior here.
        const { error } = await this.supabase
            .from('public_keys')
            .upsert(payload, { onConflict: 'user_id' });
        if (error) {
            throw new Error(`Failed to upload keys: ${error.message}`);
        }
    }
}
exports.KeyRepository = KeyRepository;
//# sourceMappingURL=KeyRepository.js.map