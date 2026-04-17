import { IKeyBundle } from '../models/KeyBundle';
import { PublicKey, PaginatedResult } from '../models/publicKey';
export interface KeySyncOptions {
    page: number;
    limit: number;
}
export declare const keyService: {
    /**
     * Fetches a paginated list of public keys.
     * Delegates storage concerns entirely to the repository layer.
     */
    syncKeys(options: KeySyncOptions): Promise<PaginatedResult<PublicKey>>;
    /**
     * Uploads a user's hybrid key bundle.
     */
    uploadKeys(keyData: IKeyBundle): Promise<void>;
};
//# sourceMappingURL=keyService.d.ts.map