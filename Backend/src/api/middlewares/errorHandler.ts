import { Request, Response, NextFunction } from 'express';

/**
 * Global error handler middleware.
 * Must be registered AFTER all routes in Express.
 * The four-argument signature is required by Express to recognise it as an error handler.
 */
export const errorHandler = (
  err: Error,
  _req: Request,
  res: Response,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  _next: NextFunction,
): void => {
  console.error('[Error]', err.stack ?? err.message);
  res.status(500).json({
    error:   'Internal server error',
    details: err.message,
  });
};
