import { Router } from 'express';
import { authController } from '../controllers';
import { validateRegister } from '../validators/registerValidator';
import { uploadLimiter } from '../middlewares/rateLimiter';

const router = Router();

/**
 * POST /auth/register
 *
 * Zero-Knowledge anonymous identity registration.
 * No Google, no OAuth, no email — purely cryptographic.
 *
 * Middleware chain:
 *   1. uploadLimiter    — max 10 registrations/min per IP (prevents floods)
 *   2. validateRegister — Zod validation of the key bundle
 *   3. authController.register — derives fingerprint and persists
 */
router.post(
  '/register',
  uploadLimiter,
  validateRegister,
  authController.register,
);

export default router;
