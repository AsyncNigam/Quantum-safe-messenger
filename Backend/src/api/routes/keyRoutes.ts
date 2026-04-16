import { Router }          from 'express';
import { keyController }    from '../controllers';
import { authMiddleware }   from '../middlewares/authMiddleware';

const router = Router();

/** GET /keys/sync?page=1&limit=20 — public, no auth required */
router.get('/sync', keyController.getPaginatedKeys);

/** POST /keys/upload — authenticated, requires valid Supabase JWT */
router.post('/upload', authMiddleware, keyController.upload);

export default router;
