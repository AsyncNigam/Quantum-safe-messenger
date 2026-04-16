import { Router } from 'express';
import { KeyController } from '../controllers/KeyController';
import { KeyRepository } from '../../repositories/KeyRepository';
import { supabase } from '../../config/supabase';
import { authMiddleware } from '../middlewares/authMiddleware';

const router = Router();

// Define dependencies for this route branch
const keyRepo = new KeyRepository(supabase);
const keyController = new KeyController(keyRepo);

/** GET /keys/sync?page=1&limit=20 */
router.get('/sync', keyController.getPaginatedKeys);

/** POST /keys/upload */
router.post('/upload', authMiddleware, keyController.upload);

export default router;
