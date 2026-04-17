"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __exportStar = (this && this.__exportStar) || function(m, exports) {
    for (var p in m) if (p !== "default" && !Object.prototype.hasOwnProperty.call(exports, p)) __createBinding(exports, m, p);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.errorHandler = exports.uploadLimiter = exports.generalLimiter = void 0;
/**
 * Central export point for all API middlewares.
 * Simplifies imports in the main server and route files.
 */
__exportStar(require("./authMiddleware"), exports);
__exportStar(require("./socketAuthMiddleware"), exports);
var rateLimiter_1 = require("./rateLimiter");
Object.defineProperty(exports, "generalLimiter", { enumerable: true, get: function () { return rateLimiter_1.generalLimiter; } });
Object.defineProperty(exports, "uploadLimiter", { enumerable: true, get: function () { return rateLimiter_1.uploadLimiter; } });
var errorHandler_1 = require("./errorHandler");
Object.defineProperty(exports, "errorHandler", { enumerable: true, get: function () { return errorHandler_1.errorHandler; } });
//# sourceMappingURL=index.js.map