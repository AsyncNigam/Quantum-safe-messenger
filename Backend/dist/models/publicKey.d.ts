/** Represents a single row in the `public_keys` Supabase table. */
export interface PublicKey {
    id: string;
    user_id: string;
    key_data: string;
    algorithm: string;
    created_at: string;
    /** Allows additional columns returned by Supabase without breaking the type. */
    [key: string]: unknown;
}
/** Generic paginated response wrapper. */
export interface PaginatedResult<T> {
    data: T[];
    page: number;
    limit: number;
    total: number;
    totalPages: number;
}
//# sourceMappingURL=publicKey.d.ts.map