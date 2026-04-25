import { z } from 'zod';
import { Request, Response, NextFunction } from 'express';

/** Base64 / URL-safe Base64 */
const base64Regex = /^[A-Za-z0-9+/\-_]+=*$/;

/**
 * Zod schema for POST /auth/register.
 *
 * We accept ONLY the two public keys required to compute the fingerprint
 * and bootstrap the hybrid KEM.  All other key material stays on-device.
 */
export const registerSchema = z.object({
  /** ML-KEM-768 public key — 1184 bytes → ~1580 Base64 chars */
  mlKemPublicKey:  z.string().min(44).regex(base64Regex, 'Must be Base64 encoded'),
  /** X25519 public key — 32 bytes → 44 Base64 chars */
  x25519PublicKey: z.string().min(44).regex(base64Regex, 'Must be Base64 encoded'),
});

export type RegisterInput = z.infer<typeof registerSchema>;

/**
 * Express middleware — validates req.body against registerSchema.
 * Replaces req.body with the sanitised, parsed object on success.
 */
export const validateRegister = (
  req: Request,
  res: Response,
  next: NextFunction,
): void => {
  const result = registerSchema.safeParse(req.body);

  if (!result.success) {
    res.status(400).json({
      error: 'Validation failed',
      issues: result.error.issues.map((i) => ({
        field:   i.path.join('.'),
        message: i.message,
      })),
    });
    return;
  }

  req.body = result.data;
  next();
};
