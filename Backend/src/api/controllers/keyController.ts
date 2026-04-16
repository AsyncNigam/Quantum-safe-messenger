import { Request, Response, NextFunction } from 'express';
import { keyService } from '../../services/keyService';

export const keyController = {
  /**
   * GET /keys/sync
   * Returns a paginated list of public keys from Supabase.
   *
   * Query params:
   *   page  — 1-indexed page number (default: 1)
   *   limit — records per page, max 100 (default: 20)
   */
  async sync(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const page  = Math.max(1, parseInt((req.query.page  as string) ?? '1',  10));
      const limit = Math.min(100, Math.max(1, parseInt((req.query.limit as string) ?? '20', 10)));

      const result = await keyService.syncKeys({ page, limit });
      res.json(result);
    } catch (err) {
      next(err);
    }
  },
};
