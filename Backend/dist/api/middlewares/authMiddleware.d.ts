import { Request, Response, NextFunction } from 'express';
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
export declare const authMiddleware: (req: Request, res: Response, next: NextFunction) => Promise<void>;
//# sourceMappingURL=authMiddleware.d.ts.map