"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.publicKeyRepository = void 0;
const supabase_1 = require("../config/supabase");
exports.publicKeyRepository = {
    /**
     * Returns a page of public keys ordered by creation date (newest first).
     * Throws on Supabase errors so the caller (service layer) can handle them.
     */
    async findPaginated(page, limit) {
        const from = (page - 1) * limit;
        const to = from + limit - 1;
        const { data, error, count } = await supabase_1.supabase
            .from('public_keys')
            .select('*', { count: 'exact' })
            .range(from, to)
            .order('created_at', { ascending: false });
        if (error)
            throw new Error(error.message);
        return {
            data: data ?? [],
            page,
            limit,
            total: count ?? 0,
            totalPages: Math.ceil((count ?? 0) / limit),
        };
    },
};
//# sourceMappingURL=publicKeyRepository.js.map