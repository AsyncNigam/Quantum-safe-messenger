import { PublicKey, PaginatedResult } from '../models/publicKey';
export declare const publicKeyRepository: {
    /**
     * Returns a page of public keys ordered by creation date (newest first).
     * Throws on Supabase errors so the caller (service layer) can handle them.
     */
    findPaginated(page: number, limit: number): Promise<PaginatedResult<PublicKey>>;
};
//# sourceMappingURL=publicKeyRepository.d.ts.map