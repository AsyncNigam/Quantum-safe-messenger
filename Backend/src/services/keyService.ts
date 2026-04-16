import { KeyRepository } from '../repositories/KeyRepository';
import { supabase } from '../config/supabase';
import { IKeyBundle } from '../models/KeyBundle';

const keyRepository = new KeyRepository(supabase);
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
    return keyRepository.findKeysPaginated(options.page, options.limit);
  },

  /**
   * Uploads a user's hybrid key bundle.
   */
  async uploadKeys(keyData: IKeyBundle): Promise<void> {
    return keyRepository.uploadKeys(keyData);
  },
};
