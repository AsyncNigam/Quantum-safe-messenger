import { Router } from 'express';
import { keyController } from '../controllers/keyController';

const router = Router();

/** GET /keys/sync?page=1&limit=20 */
router.get('/sync', keyController.sync);

export default router;
