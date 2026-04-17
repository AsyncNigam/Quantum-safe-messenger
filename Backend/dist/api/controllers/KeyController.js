"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.KeyController = void 0;
class KeyController {
    constructor(keyRepo) {
        this.keyRepo = keyRepo;
        /**
         * POST /upload
         * Authenticated route. Uploads the user's hybrid key bundle.
         */
        this.upload = async (req, res, next) => {
            try {
                const userId = req.user?.id;
                if (!userId) {
                    res.status(401).json({ error: 'User context missing' });
                    return;
                }
                // Explicitly extract fields to ignore malicious injection
                const bundle = {
                    userId,
                    x25519PublicKey: req.body.x25519PublicKey,
                    mlKemPublicKey: req.body.mlKemPublicKey,
                    ed25519Signature: req.body.ed25519Signature,
                    mlDsaSignature: req.body.mlDsaSignature,
                };
                await this.keyRepo.uploadKeys(bundle);
                res.status(201).json({ success: true, message: 'Keys uploaded successfully' });
            }
            catch (err) {
                next(err);
            }
        };
        /**
         * GET /
         * Returns a paginated list of all users' public keys for peer discovery.
         */
        this.getPaginatedKeys = async (req, res, next) => {
            try {
                const page = Math.max(1, parseInt(req.query.page || '1', 10));
                const limit = Math.min(100, Math.max(1, parseInt(req.query.limit || '20', 10)));
                const result = await this.keyRepo.findKeysPaginated(page, limit);
                res.json(result);
            }
            catch (err) {
                next(err);
            }
        };
    }
}
exports.KeyController = KeyController;
//# sourceMappingURL=KeyController.js.map