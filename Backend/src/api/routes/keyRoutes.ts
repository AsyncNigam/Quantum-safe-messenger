import { Router }             from 'express';
import { keyController }      from '../controllers';
import { authMiddleware }     from '../middlewares/authMiddleware';
import { uploadLimiter }      from '../middlewares/rateLimiter';
import { validateKeyBundle }  from '../validators/keyBundleValidator';

const router = Router();

/**
 * GET /keys/sync?page=1&limit=20
 * Public — returns paginated public keys for peer discovery.
 */
router.get('/sync', keyController.getPaginatedKeys);

/**
 * POST /keys/upload
 * Protected: authMiddleware → uploadLimiter → validateKeyBundle → controller
 *
 * Middleware chain (left to right):
 *   1. authMiddleware     — verifies Supabase JWT, attaches req.user
 *   2. uploadLimiter      — max 10 uploads/min per IP
 *   3. validateKeyBundle  — Zod schema check on req.body
 *   4. keyController.upload — writes to Supabase
 */
router.post(
  '/upload',
  authMiddleware,
  uploadLimiter,
  validateKeyBundle,
  keyController.upload,
);

export default router;
