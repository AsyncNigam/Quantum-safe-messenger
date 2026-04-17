"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const controllers_1 = require("../controllers");
const authMiddleware_1 = require("../middlewares/authMiddleware");
const rateLimiter_1 = require("../middlewares/rateLimiter");
const keyBundleValidator_1 = require("../validators/keyBundleValidator");
const router = (0, express_1.Router)();
/**
 * GET /keys/sync?page=1&limit=20
 * Public — returns paginated public keys for peer discovery.
 */
router.get('/sync', controllers_1.keyController.getPaginatedKeys);
/**
 * POST /keys/upload
 * Protected: authMiddleware → uploadLimiter → validateKeyBundle → controller
 *
 * Middleware chain (left to right):
 *   1. authMiddleware     — verifies Supabase JWT, attaches req.user
 *   2. uploadLimiter      — max 10 uploads/min per IP
 *   3. validateKeyBundle  — Zod schema check on req.body
 *   4. keyController.upload — writes to Supabase
 */
router.post('/upload', authMiddleware_1.authMiddleware, rateLimiter_1.uploadLimiter, keyBundleValidator_1.validateKeyBundle, controllers_1.keyController.upload);
exports.default = router;
//# sourceMappingURL=keyRoutes.js.map