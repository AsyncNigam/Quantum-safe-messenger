"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.errorHandler = void 0;
/**
 * Global error handler middleware.
 * Must be registered AFTER all routes in Express.
 * The four-argument signature is required by Express to recognise it as an error handler.
 */
const errorHandler = (err, _req, res, 
// eslint-disable-next-line @typescript-eslint/no-unused-vars
_next) => {
    console.error('[Error]', err.stack ?? err.message);
    res.status(500).json({
        error: 'Internal server error',
        details: err.message,
    });
};
exports.errorHandler = errorHandler;
//# sourceMappingURL=errorHandler.js.map