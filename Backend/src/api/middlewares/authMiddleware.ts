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
    console.log('[AUTH] Authorization header:', authHeader ? 'Present' : 'MISSING');
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      console.log('[AUTH] ❌ Invalid/missing auth header');
      res.status(401).json({ error: 'Missing or invalid Authorization header' });
      return;
    }

    const token = authHeader.split(' ')[1];
    console.log('[AUTH] Token length:', token.length);
    
    const { data: { user }, error } = await supabase.auth.getUser(token);

    if (error || !user) {
      console.log('[AUTH] ❌ Token validation failed:', error?.message);
      res.status(401).json({ error: 'Unauthorized', details: error?.message });
      return;
    }

    console.log('[AUTH] ✅ User authenticated:', user.id);
    req.user = user;
    next();
  } catch (err) {
    next(err);
  }
};
