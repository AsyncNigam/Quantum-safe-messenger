"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.uploadLimiter = exports.generalLimiter = void 0;
const express_rate_limit_1 = __importDefault(require("express-rate-limit"));
/**
 * General limiter — applied globally to all REST routes.
 * 100 requests per 15 minutes per IP.
 */
exports.generalLimiter = (0, express_rate_limit_1.default)({
    windowMs: 15 * 60 * 1000,
    max: 100,
    standardHeaders: true, // Return RateLimit-* headers (RFC 6585)
    legacyHeaders: false, // Disable X-RateLimit-* headers
    message: { error: 'Too many requests — please try again later.' },
});
/**
 * Strict limiter for the key upload endpoint.
 * Prevents a compromised client from flooding the key registry.
 * 10 uploads per minute per IP.
 */
exports.uploadLimiter = (0, express_rate_limit_1.default)({
    windowMs: 60 * 1000,
    max: 10,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Too many key upload requests — please try again in a minute.' },
});
//# sourceMappingURL=rateLimiter.js.map