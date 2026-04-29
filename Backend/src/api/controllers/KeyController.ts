import { Request, Response, NextFunction } from 'express';
import { KeyRepository } from '../../repositories/KeyRepository';
import { IKeyBundle } from '../../models/KeyBundle';

export class KeyController {
  constructor(private readonly keyRepo: KeyRepository) {}

  /**
   * POST /upload
   * Authenticated route. Uploads the user's hybrid key bundle.
   */
  public upload = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const fingerprint = req.user?.fingerprint;
      if (!fingerprint) {
        res.status(401).json({ error: 'User context missing' });
        return;
      }

      // Explicitly extract fields to ignore malicious injection
      const bundle: IKeyBundle = {
        fingerprint,
        x25519PublicKey: req.body.x25519PublicKey,
        mlKemPublicKey:  req.body.mlKemPublicKey,
        ed25519Signature: req.body.ed25519Signature,
        mlDsaSignature:   req.body.mlDsaSignature,
      };

      await this.keyRepo.uploadKeys(bundle);

      res.status(201).json({ success: true, message: 'Keys uploaded successfully' });
    } catch (err) {
      next(err);
    }
  };

  /**
   * GET /
   * Returns a paginated list of all users' public keys for peer discovery.
   */
  public getPaginatedKeys = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const page = Math.max(1, parseInt((req.query.page as string) || '1', 10));
      const limit = Math.min(100, Math.max(1, parseInt((req.query.limit as string) || '20', 10)));

      const result = await this.keyRepo.findKeysPaginated(page, limit);
      res.json(result);
    } catch (err) {
      next(err);
    }
  };
}
