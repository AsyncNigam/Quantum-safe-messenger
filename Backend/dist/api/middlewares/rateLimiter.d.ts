/**
 * General limiter — applied globally to all REST routes.
 * 100 requests per 15 minutes per IP.
 */
export declare const generalLimiter: import("express-rate-limit").RateLimitRequestHandler;
/**
 * Strict limiter for the key upload endpoint.
 * Prevents a compromised client from flooding the key registry.
 * 10 uploads per minute per IP.
 */
export declare const uploadLimiter: import("express-rate-limit").RateLimitRequestHandler;
//# sourceMappingURL=rateLimiter.d.ts.map