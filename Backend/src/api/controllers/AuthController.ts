import { Request, Response, NextFunction } from 'express';
import { createHash } from 'crypto';
import { UserRepository } from '../../repositories/UserRepository';
import { FcmService } from '../../services/fcmService';
import { RegisterInput } from '../validators/registerValidator';

export class AuthController {
  constructor(
    private readonly userRepo: UserRepository,
    private readonly fcmService: FcmService,
  ) {}

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

  /**
   * GET /auth/lookup/:fingerprint
   *
   * Contact discovery — returns the public key bundle for a given fingerprint.
   * The requesting user must be authenticated (authMiddleware).
   */
  public lookup = async (
    req: Request,
    res: Response,
    next: NextFunction,
  ): Promise<void> => {
    try {
      const fingerprint = req.params.fingerprint as string;

      if (!fingerprint || fingerprint.length !== 64 || !/^[a-f0-9]+$/.test(fingerprint)) {
        res.status(400).json({ error: 'Invalid fingerprint format' });
        return;
      }

      const user = await this.userRepo.findByFingerprint(fingerprint);

      if (!user) {
        res.status(404).json({ error: 'User not found' });
        return;
      }

      console.log(`[Auth] 🔍 Lookup | fingerprint=${fingerprint.slice(0, 12)}…`);

      res.status(200).json({
        success: true,
        fingerprint:     user.fingerprint,
        mlKemPublicKey:  user.mlKemPublicKey,
        x25519PublicKey: user.x25519PublicKey,
      });
    } catch (err) {
      next(err);
    }
  };

  /**
   * POST /auth/fcm-token
   *
   * Register or update the FCM push notification token for a user.
   * Requires Bearer auth (fingerprint).
   */
  public registerFcmToken = async (
    req: Request,
    res: Response,
    next: NextFunction,
  ): Promise<void> => {
    try {
      const fingerprint = (req as any).fingerprint as string;
      const { fcmToken } = req.body;

      if (!fcmToken || typeof fcmToken !== 'string' || fcmToken.length < 10) {
        res.status(400).json({ error: 'Invalid FCM token' });
        return;
      }

      await this.fcmService.upsertToken(fingerprint, fcmToken);

      console.log(`[Auth] 📱 FCM token registered | fingerprint=${fingerprint.slice(0, 12)}…`);

      res.status(200).json({
        success: true,
        message: 'FCM token registered',
      });
    } catch (err) {
      next(err);
    }
  };
}

