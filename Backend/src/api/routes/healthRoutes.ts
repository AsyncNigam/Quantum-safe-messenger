import { Router, Request, Response } from 'express';

const router = Router();

/** GET /health — basic liveness probe */
router.get('/', (_req: Request, res: Response) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

export default router;
