import { Router } from 'express';
import { authController } from '../controllers';
import { validateRegister } from '../validators/registerValidator';
import { uploadLimiter } from '../middlewares/rateLimiter';
import { authMiddleware } from '../middlewares/authMiddleware';

const router = Router();

/**
 * POST /auth/register
 *
 * Zero-Knowledge anonymous identity registration.
 * No Google, no OAuth, no email — purely cryptographic.
 */
router.post(
  '/register',
  uploadLimiter,
  validateRegister,
  authController.register,
);

/**
 * GET /auth/lookup/:fingerprint
 *
 * Contact discovery — returns ML-KEM and X25519 public keys for
 * a given text fingerprint. Requires the caller to be authenticated.
 */
router.get(
  '/lookup/:fingerprint',
  authMiddleware,
  authController.lookup,
);

export default router;
