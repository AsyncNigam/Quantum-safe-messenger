import { Request, Response, NextFunction } from 'express';
import { supabase } from '../../config/supabase';

declare global {
  namespace Express {
    interface Request {
      user?: import('@supabase/supabase-js').User;
    }
  }
}

/**
 * Validates the Supabase JWT provided in the Authorization header.
 * Attaches the authenticated user to the Request object.
 */
export const authMiddleware = async (
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      res.status(401).json({ error: 'Missing or invalid Authorization header' });
      return;
    }

    const token = authHeader.split(' ')[1];
    const { data: { user }, error } = await supabase.auth.getUser(token);

    if (error || !user) {
      res.status(401).json({ error: 'Unauthorized', details: error?.message });
      return;
    }

    req.user = user;
    next();
  } catch (err) {
    next(err);
  }
};
