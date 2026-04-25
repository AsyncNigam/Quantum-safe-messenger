import { Request, Response, NextFunction } from 'express';
import { createHash } from 'crypto';
import { UserRepository } from '../../repositories/UserRepository';
import { RegisterInput } from '../validators/registerValidator';

export class AuthController {
  constructor(private readonly userRepo: UserRepository) {}

  /**
   * POST /auth/register
   *
   * Zero-Knowledge registration — no account, no email, no password.
   * The client sends its ML-KEM and X25519 public keys.
   * The server derives a deterministic fingerprint and stores the key bundle.
   *
   * Fingerprint = SHA-256( base64_decode(mlKemPublicKey) || base64_decode(x25519PublicKey) )
   * encoded as lowercase hex (64 characters).
   */
  public register = async (
    req: Request,
    res: Response,
    next: NextFunction,
  ): Promise<void> => {
    try {
      const { mlKemPublicKey, x25519PublicKey } = req.body as RegisterInput;

      // Derive the Text Fingerprint / Identity
      const fingerprint = createHash('sha256')
        .update(Buffer.from(mlKemPublicKey, 'base64'))
        .update(Buffer.from(x25519PublicKey, 'base64'))
        .digest('hex');

      // Idempotent upsert — re-registering with the same keys is harmless
      await this.userRepo.upsertUser({ fingerprint, mlKemPublicKey, x25519PublicKey });

      console.log(`[Auth] ✅ Registered | fingerprint=${fingerprint.slice(0, 12)}…`);

      res.status(201).json({
        success: true,
        textFingerprint: fingerprint,
      });
    } catch (err) {
      next(err);
    }
  };
}
