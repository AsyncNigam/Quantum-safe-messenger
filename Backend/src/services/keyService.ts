import { publicKeyRepository } from '../repositories/publicKeyRepository';
import { PublicKey, PaginatedResult } from '../models/publicKey';

export interface KeySyncOptions {
  page: number;
  limit: number;
}

export const keyService = {
  /**
   * Fetches a paginated list of public keys.
   * Delegates storage concerns entirely to the repository layer.
   */
  async syncKeys(options: KeySyncOptions): Promise<PaginatedResult<PublicKey>> {
    return publicKeyRepository.findPaginated(options.page, options.limit);
  },
};
