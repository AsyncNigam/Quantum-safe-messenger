"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.authMiddleware = void 0;
const supabase_1 = require("../../config/supabase");
/**
 * Validates the Supabase JWT provided in the Authorization header.
 * Attaches the authenticated user to the Request object.
 */
const authMiddleware = async (req, res, next) => {
    try {
        const authHeader = req.headers.authorization;
        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            res.status(401).json({ error: 'Missing or invalid Authorization header' });
            return;
        }
        const token = authHeader.split(' ')[1];
        const { data: { user }, error } = await supabase_1.supabase.auth.getUser(token);
        if (error || !user) {
            res.status(401).json({ error: 'Unauthorized', details: error?.message });
            return;
        }
        req.user = user;
        next();
    }
    catch (err) {
        next(err);
    }
};
exports.authMiddleware = authMiddleware;
//# sourceMappingURL=authMiddleware.js.map