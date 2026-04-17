import { Request, Response, NextFunction } from 'express';
/**
 * Global error handler middleware.
 * Must be registered AFTER all routes in Express.
 * The four-argument signature is required by Express to recognise it as an error handler.
 */
export declare const errorHandler: (err: Error, _req: Request, res: Response, _next: NextFunction) => void;
//# sourceMappingURL=errorHandler.d.ts.map