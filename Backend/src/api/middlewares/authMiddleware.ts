import { Request, Response, NextFunction } from 'express';
import { userRepository } from '../../repositories';

// Extend Express Request to carry the verified fingerprint
declare global {
  namespace Express {
    interface Request {
      user?: { fingerprint: string };
    }
  }
}

/**
 * HTTP authentication middleware — Zero-Knowledge fingerprint model.
 *
 * Clients must send:
 *   Authorization: Bearer <textFingerprint>
 *
 * The fingerprint is validated against the `users` table (via the admin client).
 * No JWT, no OAuth, no external auth provider.
 */
export const authMiddleware = async (
  req: Request,
  res: Response,
  next: NextFunction,
): Promise<void> => {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      res.status(401).json({ error: 'Missing or invalid Authorization header' });
      return;
    }

    const fingerprint = authHeader.split(' ')[1];

    if (!fingerprint || fingerprint.length !== 64 || !/^[a-f0-9]+$/.test(fingerprint)) {
      res.status(401).json({ error: 'Invalid fingerprint format' });
      return;
    }

    const user = await userRepository.findByFingerprint(fingerprint);

    if (!user) {
      res.status(401).json({ error: 'Unrecognised identity — please register first' });
      return;
    }

    req.user = { fingerprint: user.fingerprint };
    next();
  } catch (err) {
    next(err);
  }
};
